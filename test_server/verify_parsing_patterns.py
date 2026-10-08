#!/usr/bin/env python3
"""
Script de verificación de expresiones regulares para notificaciones de Yape Perú.
Valida los patrones implementados en YapeNotificationParser.kt.
"""

import re

CASES = [
    {
        "title": "Yape",
        "text": "¡Te transfirieron S/ 15.50 de Juan Perez!",
        "expected_amount": 15.50,
        "expected_sender": "Juan Perez",
        "expected_code": None
    },
    {
        "title": "Yape",
        "text": "Juan Perez te transfirió S/ 15.50",
        "expected_amount": 15.50,
        "expected_sender": "Juan Perez",
        "expected_code": None
    },
    {
        "title": "Confirmación de pago",
        "text": "Juan Carlos Perez te envió S/ 25.00 con código de seguridad 849201",
        "expected_amount": 25.00,
        "expected_sender": "Juan Carlos Perez",
        "expected_code": "849201"
    },
    {
        "title": "Yape",
        "text": "Has recibido S/ 35.00 de Pedro Quispe. Cód: 981240",
        "expected_amount": 35.00,
        "expected_sender": "Pedro Quispe",
        "expected_code": "981240"
    },
    {
        "title": "María Lopez",
        "text": "Te envió S/ 50.00",
        "expected_amount": 50.00,
        "expected_sender": "María Lopez",
        "expected_code": None
    }
]

AMOUNT_PATTERN = re.compile(r'(?:S\/?\.?|Soles)\s*([0-9]+(?:[.,][0-9]{1,2})?)', re.IGNORECASE)
SECURITY_CODE_PATTERN = re.compile(r'(?:código(?: de seguridad| de validación)?|cód\.?\s*seg\.?|cód\.?|cod\.?)\s*[:#]?\s*([0-9a-zA-Z]{4,10})', re.IGNORECASE)
SENDER_AFTER_DE_PATTERN = re.compile(r'\b(?:de|por)\s+([A-Za-zÁÉÍÓÚáéíóúÑñ\s\.\'\-]{3,45}?)(?:\s*(?:con\s+código|cód|\.|\!|\?|$))', re.IGNORECASE)
SENDER_BEFORE_ACTION_PATTERN = re.compile(r'(?:^|[!¡\n])\s*([A-Za-zÁÉÍÓÚáéíóúÑñ\s\.\'\-]{3,40}?)\s+(?:te transfirió|te transfirieron|te envió|te yapeó|te yapearon)', re.IGNORECASE)

GENERIC_TITLES = {"yape", "bcp", "confirmación de pago", "pago recibido", "notificación"}

def clean_sender(raw: str) -> str:
    if not raw:
        return ""
    cleaned = re.sub(r'^(?:yape|bcp)\s+', '', raw.strip(), flags=re.IGNORECASE)
    cleaned = re.sub(r'^[\s!¡\-:;,\.]+', '', cleaned)
    cleaned = re.sub(r'[\s!¡\-:;,\.]+$', '', cleaned)
    cleaned = re.sub(r'\s+', ' ', cleaned)
    return cleaned.strip()

def is_generic_title(title: str) -> bool:
    t = title.strip().lower()
    return any(g in t for g in GENERIC_TITLES)

def extract_sender(title: str, text: str, full: str) -> str:
    # 1. Probar en text primero (donde usualmente viene la frase completa del pago)
    m_de = SENDER_AFTER_DE_PATTERN.search(text)
    if m_de:
        c = clean_sender(m_de.group(1))
        if len(c) >= 3 and not is_generic_title(c):
            return c

    m_bef = SENDER_BEFORE_ACTION_PATTERN.search(text)
    if m_bef:
        c = clean_sender(m_bef.group(1))
        if len(c) >= 3 and not is_generic_title(c):
            return c

    # 2. Si el título no es genérico, el remitente suele ser el título (ej: "María Lopez")
    if title and not is_generic_title(title):
        c = clean_sender(title)
        if len(c) >= 3:
            return c

    # 3. Fallback en fullMessage
    m_de_full = SENDER_AFTER_DE_PATTERN.search(full)
    if m_de_full:
        c = clean_sender(m_de_full.group(1))
        if len(c) >= 3 and not is_generic_title(c):
            return c

    m_bef_full = SENDER_BEFORE_ACTION_PATTERN.search(full)
    if m_bef_full:
        c = clean_sender(m_bef_full.group(1))
        if len(c) >= 3 and not is_generic_title(c):
            return c

    return "Cliente Yape"

def test_cases():
    print("Iniciando validación de patrones de parsing...")
    for i, c in enumerate(CASES, 1):
        title = c['title']
        text = c['text']
        full = f"{title} {text}".strip()

        # Monto
        m_amt = AMOUNT_PATTERN.search(full)
        amt = float(m_amt.group(1).replace(',', '.')) if m_amt else None

        # Codigo
        m_code = SECURITY_CODE_PATTERN.search(full)
        code = m_code.group(1).strip() if m_code else None

        # Sender
        sender = extract_sender(title, text, full)

        assert amt == c['expected_amount'], f"Fallo monto caso {i}: {amt} != {c['expected_amount']}"
        assert code == c['expected_code'], f"Fallo código caso {i}: {code} != {c['expected_code']}"
        assert sender == c['expected_sender'], f"Fallo remitente caso {i}: '{sender}' != '{c['expected_sender']}'"
        print(f" Caso {i} OK: Monto=S/{amt:.2f}, Remitente='{sender}', Código={code}")

    print("\n¡Todos los patrones validados satisfactoriamente!")

if __name__ == '__main__':
    test_cases()
