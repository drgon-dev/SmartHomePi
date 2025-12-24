# server_object.py на Raspberry Pi
import bluetooth
import json
import time

def parse_object_data(json_str):
    """Парсинг JSON данных от Android"""
    try:
        data = json.loads(json_str)

        # Проверяем наличие маркеров
        if isinstance(data, str) and data.startswith("START_JSON:"):
            # Убираем маркеры
            clean_json = data.replace("START_JSON:", "").replace(":END_JSON", "")
            data = json.loads(clean_json)

        # Обрабатываем данные в зависимости от типа
        if isinstance(data, dict) and 'type' in data:
            object_type = data['type']
            object_data = data['data']
            timestamp = data.get('timestamp', 0)

            print(f"\nПолучен объект типа: {object_type}")
            print(f"Время отправки: {time.ctime(timestamp / 1000)}")

            # Обработка разных типов объектов
            if object_type == "SensorData":
                print(f"Данные сенсора:")
                print(f"  Устройство: {object_data.get('deviceName')}")
                print(f"  Температура: {object_data.get('temperature')}°C")
                print(f"  Влажность: {object_data.get('humidity')}%")
                print(f"  Сигнал: {object_data.get('signalStrength')}%")

            elif object_type == "ControlCommand":
                print(f"Команда управления:")
                print(f"  Команда: {object_data.get('command')}")
                print(f"  Цель: {object_data.get('target')}")
                print(f"  Значение: {object_data.get('value')}")
                print(f"  Параметры: {object_data.get('parameters')}")

                # Выполняем действие
                execute_command(object_data)

            elif object_type == "UserData":
                print(f"Пользовательские данные:")
                print(f"  Имя: {object_data.get('username')}")
                print(f"  ID: {object_data.get('userId')}")
                print(f"  Координаты: {object_data.get('latitude')}, {object_data.get('longitude')}")

            else:
                print(f"Неизвестный тип объекта. Данные: {object_data}")

        elif isinstance(data, dict) and 'className' in data:
            # Обработка универсального контейнера
            print(f"\nПолучен объект класса: {data['className']}")
            print(f"Данные: {data['data']}")

        else:
            print(f"Получены данные: {data}")

    except json.JSONDecodeError as e:
        print(f"Ошибка парсинга JSON: {e}")
    except Exception as e:
        print(f"Ошибка обработки: {e}")

def execute_command(command_data):
    """Выполнение команды на Raspberry Pi"""
    cmd = command_data.get('command', '')
    target = command_data.get('target', '')
    value = command_data.get('value', 0)

    if cmd == "SET_LED":
        # Пример: управление GPIO
        print(f"  Выполняю: Установка {target} в {value}")
        # Здесь код для управления GPIO
        # import RPi.GPIO as GPIO
        # GPIO.setup(pin, GPIO.OUT)
        # GPIO.output(pin, value)

    elif cmd == "SET_PWM":
        print(f"  Выполняю: Установка PWM {target} в {value}")
        # Код для PWM

    elif cmd == "READ_SENSOR":
        print(f"  Выполняю: Чтение сенсора {target}")

def start_bluetooth_server():
    """Запуск Bluetooth сервера для приема объектов"""
    server_socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    server_socket.bind(("", 1))
    server_socket.listen(1)

    print("Сервер для приема объектов запущен")
    print("Ожидание подключения Android...")

    try:
        while True:
            client_socket, address = server_socket.accept()
            print(f"\nПодключен: {address}")

            try:
                buffer = ""
                while True:
                    data = client_socket.recv(1024).decode('utf-8')
                    if not data:
                        break

                    buffer += data

                    # Обрабатываем каждую завершенную строку
                    while '\n' in buffer:
                        line, buffer = buffer.split('\n', 1)
                        if line.strip():
                            parse_object_data(line.strip())

            except Exception as e:
                print(f"Ошибка в соединении: {e}")
            finally:
                client_socket.close()
                print("Соединение закрыто")

    except KeyboardInterrupt:
        print("\nСервер остановлен")
    finally:
        server_socket.close()

if __name__ == "__main__":
    start_bluetooth_server()