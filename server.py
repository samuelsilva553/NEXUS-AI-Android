import json
import os
import subprocess
import time
import urllib.request
import urllib.error
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

HOME = Path.home()
MODEL = HOME / "nexus-ai" / "modelos" / "nexus.gguf"
PROMPT_FILE = HOME / "nexus-ai" / "programador.txt"
LLAMA_HOST = "127.0.0.1"
LLAMA_PORT = 8080
APP_PORT = 8000

if not MODEL.is_file():
    raise SystemExit(f"Modelo não encontrado: {MODEL}")
if not PROMPT_FILE.is_file():
    raise SystemExit(f"Prompt não encontrado: {PROMPT_FILE}")

SYSTEM_PROMPT = PROMPT_FILE.read_text(encoding="utf-8").strip()
llama_process = None

def start_llama():
    global llama_process

    print("Iniciando o motor da NEXUS AI...", flush=True)
    llama_process = subprocess.Popen([
        "llama-server",
        "--model", str(MODEL),
        "--host", LLAMA_HOST,
        "--port", str(LLAMA_PORT),
        "--alias", "nexus",
        "--ctx-size", "2048"
    ])

    health_url = f"http://{LLAMA_HOST}:{LLAMA_PORT}/health"

    for _ in range(180):
        if llama_process.poll() is not None:
            raise RuntimeError(
                f"llama-server encerrou com código {llama_process.returncode}"
            )
        try:
            with urllib.request.urlopen(health_url, timeout=2) as response:
                if response.status == 200:
                    print("Motor da IA pronto!", flush=True)
                    return
        except Exception:
            pass
        time.sleep(2)

    llama_process.terminate()
    raise RuntimeError("Tempo esgotado aguardando o motor da IA.")

class Handler(BaseHTTPRequestHandler):
    def send_json(self, status, data):
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/health":
            self.send_json(200, {"status": "online", "name": "NEXUS AI"})
        else:
            self.send_json(404, {"error": "Rota não encontrada"})

    def do_POST(self):
        if self.path != "/chat":
            self.send_json(404, {"error": "Rota não encontrada"})
            return

        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length < 1 or length > 100000:
                self.send_json(400, {"error": "Mensagem vazia ou muito grande"})
                return

            incoming = json.loads(self.rfile.read(length).decode("utf-8"))
            message = str(incoming.get("message", "")).strip()

            if not message:
                self.send_json(400, {"error": "Digite uma mensagem"})
                return

            payload = {
                "model": "nexus",
                "messages": [
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": message}
                ],
                "stream": False,
                "temperature": 0.7,
                "max_tokens": 512
            }

            request = urllib.request.Request(
                f"http://{LLAMA_HOST}:{LLAMA_PORT}/v1/chat/completions",
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST"
            )

            with urllib.request.urlopen(request, timeout=300) as response:
                result = json.loads(response.read().decode("utf-8"))

            answer = result["choices"][0]["message"]["content"].strip()
            self.send_json(200, {"response": answer})

        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")[:1000]
            print(f"Erro HTTP do motor: {exc.code} {detail}", flush=True)
            self.send_json(502, {"error": "O motor da IA recusou a solicitação"})
        except Exception as exc:
            print(f"Erro ao responder: {exc}", flush=True)
            self.send_json(500, {"error": "Falha ao gerar resposta"})

    def log_message(self, fmt, *args):
        print("[NEXUS] " + fmt % args, flush=True)

try:
    start_llama()
    server = ThreadingHTTPServer(("127.0.0.1", APP_PORT), Handler)
    print(f"NEXUS AI pronta em http://127.0.0.1:{APP_PORT}", flush=True)
    print("Mantenha esta sessão do Termux aberta.", flush=True)
    server.serve_forever()
except KeyboardInterrupt:
    print("\nEncerrando NEXUS AI...", flush=True)
finally:
    if llama_process and llama_process.poll() is None:
        llama_process.terminate()
