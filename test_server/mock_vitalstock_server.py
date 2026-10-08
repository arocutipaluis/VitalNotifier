#!/usr/bin/env python3
"""
Servidor Mock de Prueba para VitalStock (Escritorio - Botica)
Permite verificar la recepción de pagos de VitalNotifier en la computadora local.

Uso:
    python mock_vitalstock_server.py
    (Por defecto escucha en el puerto 5005 en todas las interfaces de red local 0.0.0.0)
"""

import http.server
import json
import socket
import sys
from datetime import datetime

PORT = 5005

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # No necesita ser alcanzable realmente
        s.connect(('8.8.8.8', 1))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

class VitalStockHandler(http.server.BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == '/ping':
            response = {
                "status": "online",
                "service": "VitalStock Desktop Botica",
                "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            }
            payload = json.dumps(response).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
            print(f"[PING] Conexión probada desde el teléfono {self.client_address[0]} -> OK")
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == '/yape':
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length).decode('utf-8')

            try:
                data = json.loads(post_data)
                remitente = data.get("remitente", "Desconocido")
                monto = data.get("monto", 0.0)
                fecha_hora = data.get("fecha_hora", "")
                codigo_seg = data.get("codigo_seg", "Sin código")
                raw = data.get("mensaje_raw", "")

                print("\n" + "=" * 55)
                print("  [VITALSTOCK] ¡NUEVO PAGO DE YAPE RECIBIDO!")
                print("=" * 55)
                print(f"  Remitente:     {remitente}")
                print(f"  Monto:         S/ {monto:.2f}")
                print(f"  Fecha y Hora:  {fecha_hora}")
                print(f"  Código Seg:    {codigo_seg}")
                print(f"  Texto Raw:     {raw}")
                print(f"  IP Teléfono:   {self.client_address[0]}")
                print("=" * 55 + "\n")

                response = {
                    "success": True,
                    "message": f"Pago de S/ {monto:.2f} registrado exitosamente en VitalStock",
                    "remitente": remitente
                }
                payload = json.dumps(response).encode('utf-8')
                self.send_response(200)
                self.send_header('Content-Type', 'application/json; charset=utf-8')
                self.send_header('Content-Length', str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

            except json.JSONDecodeError:
                self.send_response(400)
                self.end_headers()
                print("[ERROR] Formato JSON inválido recibido")
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        # Desactivar logs por defecto de BaseHTTPRequestHandler para salida limpia
        pass

def main():
    local_ip = get_local_ip()
    server_address = ('0.0.0.0', PORT)
    httpd = http.server.HTTPServer(server_address, VitalStockHandler)

    print("=" * 60)
    print("  SERVIDOR MOCK VITALSTOCK (ESCRITORIO BOTICA)")
    print("=" * 60)
    print(f"  Puerto:          {PORT}")
    print(f"  IP sugerida:     {local_ip}")
    print(f"  Endpoint Ping:   http://{local_ip}:{PORT}/ping")
    print(f"  Endpoint Yape:   http://{local_ip}:{PORT}/yape")
    print("=" * 60)
    print("  --> Coloca esta IP en la app móvil VitalNotifier:")
    print(f"      IP de la PC: {local_ip}")
    print(f"      Puerto:      {PORT}")
    print("=" * 60)
    print("Esperando notificaciones de Yape... (Presiona Ctrl+C para detener)\n")

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nServidor cerrado.")
        httpd.server_close()

if __name__ == '__main__':
    main()
