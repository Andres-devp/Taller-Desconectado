.PHONY: up down clean logs test seed apk ip

up:            ## Levanta postgres + api + visor
	docker compose up -d --build

down:
	docker compose down

clean:         ## Detiene y borra los datos
	docker compose down -v

logs:
	docker compose logs -f api

test:          ## Pruebas del sistema central y de la app Android
	cd sincronizacion && (test -x .venv/bin/pytest || (python3 -m venv .venv && .venv/bin/pip install -q -r requirements-dev.txt)) && .venv/bin/pytest
	cd app-android && ./gradlew testDebugUnitTest

seed:          ## Carga reportes de ejemplo a través de la API
	python3 sincronizacion/scripts/seed.py http://localhost:8000 400

apk:           ## Compila la APK y la copia a apk/
	cd app-android && ./gradlew assembleDebug
	mkdir -p apk && cp app-android/app/build/outputs/apk/debug/app-debug.apk apk/taller-desconectado.apk

ip:            ## IP de este equipo en la LAN (para configurar la app en un celular)
	@ipconfig getifaddr en0 2>/dev/null || hostname -I | awk '{print $$1}'
