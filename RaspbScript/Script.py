import json
import struct
import bluetooth
import RPi.GPIO as GPIO
import threading
import time
from datetime import datetime

class DeviceDataReceiver:
    def __init__(self, port=1):
        GPIO.setmode(GPIO.BCM)
        GPIO.setwarnings(True)

        self.server_socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        self.server_socket.bind(("", port))
        self.server_socket.listen(1)
        self.port = port

        self.device_types = {
            0: "LIGHT",
            1: "THERMOSTAT",
            2: "SECURITY_CAMERA",
            3: "LOCK",
            4: "BLINDS",
            5: "SOCKET",
            6: "SENSOR"
        }

        self.device_pins = {
            "LIGHT": {
                "default_pin": 17,      # GPIO17 свет
                "pwm_pin": 18,          # GPIO18 яркость
                "rgb_pins": [22, 23, 24] # GPIO для RGB
            },
            "SOCKET": {
                "default_pin": 4        # GPIO4 я розетка
            },

            "LOCK": {
                "default_pin": 27,
                "feedback_pin": 22
            },

            "BLINDS": {
                "motor_pin1": 5,        # GPIO5 управление подъёмником
                "motor_pin2": 6,        # GPIO6 напрвавление
                "limit_switch_up": 13,  # GPIO13 стоп верх
                "limit_switch_down": 19 # GPIO19 стоп низ
            },
            "SENSOR": {
                "input_pin": 26
            }
        }

        self.pwm_objects = {}
        self.initialize_gpio()

        self.device_states = {}

        self.temperature_monitoring = False
        self.current_temp = 22.0

        print(f"Bluetooth сервер запущен на порту {port}")
        print("GPIO инициализирован")
        print("Ожидание подключения...")

    def initialize_gpio(self):
        """Инициализация GPIO пинов для всех устройств"""
        for device_type, pins in self.device_pins.items():
            if device_type == "LIGHT":
                GPIO.setup(pins["default_pin"], GPIO.OUT, initial=GPIO.LOW)
                GPIO.setup(pins["pwm_pin"], GPIO.OUT, initial=GPIO.LOW)

                self.pwm_objects["light_pwm"] = GPIO.PWM(pins["pwm_pin"], 1000)
                self.pwm_objects["light_pwm"].start(0)


                for pin in pins["rgb_pins"]:
                    GPIO.setup(pin, GPIO.OUT, initial=GPIO.LOW)
                    self.pwm_objects[f"rgb_{pin}"] = GPIO.PWM(pin, 1000)
                    self.pwm_objects[f"rgb_{pin}"].start(0)

            elif device_type == "SOCKET":
                GPIO.setup(pins["default_pin"], GPIO.OUT, initial=GPIO.LOW)

            elif device_type == "LOCK":
                GPIO.setup(pins["default_pin"], GPIO.OUT, initial=GPIO.LOW)
                GPIO.setup(pins["feedback_pin"], GPIO.IN, pull_up_down=GPIO.PUD_UP)

            elif device_type == "BLINDS":
                GPIO.setup(pins["motor_pin1"], GPIO.OUT, initial=GPIO.LOW)
                GPIO.setup(pins["motor_pin2"], GPIO.OUT, initial=GPIO.LOW)
                GPIO.setup(pins["limit_switch_up"], GPIO.IN, pull_up_down=GPIO.PUD_UP)
                GPIO.setup(pins["limit_switch_down"], GPIO.IN, pull_up_down=GPIO.PUD_UP)

            elif device_type == "SENSOR":
                GPIO.setup(pins["input_pin"], GPIO.IN, pull_up_down=GPIO.PUD_UP)

    def parse_device_data(self, data_bytes):
        """Парсинг данных устройства из байтовой строки"""
        try:
            device_data = {}
            offset = 0

            id_length = data_bytes[offset]
            offset += 1
            device_data['id'] = data_bytes[offset:offset + id_length].decode('utf-8')
            offset += id_length

            name_length = data_bytes[offset]
            offset += 1
            device_data['name'] = data_bytes[offset:offset + name_length].decode('utf-8')
            offset += name_length

            type_code = data_bytes[offset]
            offset += 1
            device_data['type'] = self.device_types.get(type_code, "UNKNOWN")

            status_length = data_bytes[offset]
            offset += 1
            device_data['status'] = data_bytes[offset:offset + status_length].decode('utf-8')
            offset += status_length

            device_data['isActive'] = bool(data_bytes[offset])
            offset += 1

            device_data['iconRes'] = int.from_bytes(data_bytes[offset:offset+4], byteorder='little')
            offset += 4

            if offset < len(data_bytes):
                if device_data['type'] == "LIGHT":
                    device_data['brightness'] = data_bytes[offset]
                    offset += 1

                    color_length = data_bytes[offset]
                    offset += 1
                    device_data['color'] = data_bytes[offset:offset + color_length].decode('utf-8')
                    offset += color_length

                elif device_data['type'] == "THERMOSTAT":
                    mode_length = data_bytes[offset]
                    offset += 1
                    device_data['acMode'] = data_bytes[offset:offset + mode_length].decode('utf-8')
                    offset += mode_length

                    device_data['acTemperature'] = data_bytes[offset]
                    offset += 1

                    fan_speed_length = data_bytes[offset]
                    offset += 1
                    device_data['acFanSpeed'] = data_bytes[offset:offset + fan_speed_length].decode('utf-8')
                    offset += fan_speed_length

            self.process_device_with_gpio(device_data)

            return device_data

        except Exception as e:
            print(f"Ошибка при парсинге данных: {e}")
            return None

    def process_device_with_gpio(self, device_data):
        """Обработка устройства с использованием GPIO"""
        device_type = device_data['type']
        device_id = device_data['id']
        is_active = device_data['isActive']

        print(f"Обработка устройства: {device_id} ({device_type}), Активно: {is_active}")

        self.device_states[device_id] = device_data

        if device_type == "LIGHT":
            self.control_light(device_data)

        elif device_type == "SOCKET":
            self.control_socket(device_data)

        elif device_type == "LOCK":
            self.control_lock(device_data)

        elif device_type == "BLINDS":
            self.control_blinds(device_data)

        elif device_type == "THERMOSTAT":
            self.control_thermostat(device_data)

        elif device_type == "SENSOR":
            self.read_sensor_data(device_data)

        elif device_type == "SECURITY_CAMERA":
            self.control_camera(device_data)

    def control_light(self, device_data):
        """Управление светом через GPIO"""
        pins = self.device_pins.get("LIGHT")
        if not pins:
            print("Конфигурация пинов для света не найдена")
            return

        is_active = device_data['isActive']

        GPIO.output(pins["default_pin"], GPIO.HIGH if is_active else GPIO.LOW)

        if 'brightness' in device_data:
            brightness = device_data['brightness']
            duty_cycle = (brightness / 255.0) * 100
            self.pwm_objects["light_pwm"].ChangeDutyCycle(duty_cycle)
            print(f"Установлена яркость: {brightness} ({duty_cycle:.1f}%)")

        if 'color' in device_data:
            color = device_data['color']
            if color.startswith('#'):
                hex_color = color.lstrip('#')
                rgb = tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))

                rgb_pins = pins["rgb_pins"]
                for i, pin in enumerate(rgb_pins):
                    if i < len(rgb):
                        duty_cycle = (rgb[i] / 255.0) * 100
                        self.pwm_objects[f"rgb_{pin}"].ChangeDutyCycle(duty_cycle)

                print(f"Установлен цвет RGB: {rgb}")

    def control_socket(self, device_data):
        """Управление розеткой через GPIO"""
        pins = self.device_pins.get("SOCKET")
        if not pins:
            print("Конфигурация пинов для розетки не найдена")
            return

        is_active = device_data['isActive']
        GPIO.output(pins["default_pin"], GPIO.HIGH if is_active else GPIO.LOW)
        print(f"Розетка {'ВКЛЮЧЕНА' if is_active else 'ВЫКЛЮЧЕНА'}")

    def control_lock(self, device_data):
        """Управление замком через GPIO"""
        pins = self.device_pins.get("LOCK")
        if not pins:
            print("Конфигурация пинов для замка не найдена")
            return

        GPIO.output(pins["default_pin"], GPIO.HIGH)
        print("Замок активирован")

        threading.Timer(2.0, lambda: GPIO.output(pins["default_pin"], GPIO.LOW)).start()

        door_status = GPIO.input(pins["feedback_pin"])
        device_data['door_status'] = "CLOSED" if door_status else "OPEN"
        print(f"Статус двери: {device_data['door_status']}")

    def control_blinds(self, device_data):
        """Управление шторами через GPIO"""
        pins = self.device_pins.get("BLINDS")
        if not pins:
            print("Конфигурация пинов для штор не найдена")
            return

        status = device_data['status'].upper()

        if status == "UP":
            GPIO.output(pins["motor_pin1"], GPIO.HIGH)
            GPIO.output(pins["motor_pin2"], GPIO.LOW)

            def stop_at_limit():
                while GPIO.input(pins["limit_switch_up"]):
                    time.sleep(0.1)
                GPIO.output(pins["motor_pin1"], GPIO.LOW)
                GPIO.output(pins["motor_pin2"], GPIO.LOW)
                print("Шторы подняты")

            threading.Thread(target=stop_at_limit).start()

        elif status == "DOWN":
            GPIO.output(pins["motor_pin1"], GPIO.LOW)
            GPIO.output(pins["motor_pin2"], GPIO.HIGH)

            def stop_at_limit():
                while GPIO.input(pins["limit_switch_down"]):
                    time.sleep(0.1)
                GPIO.output(pins["motor_pin1"], GPIO.LOW)
                GPIO.output(pins["motor_pin2"], GPIO.LOW)
                print("Шторы опущены")

            threading.Thread(target=stop_at_limit).start()

        elif status == "STOP":
            GPIO.output(pins["motor_pin1"], GPIO.LOW)
            GPIO.output(pins["motor_pin2"], GPIO.LOW)
            print("Шторы остановлены")

    def control_thermostat(self, device_data):
        """Имитация управления термостатом"""

        if 'acTemperature' in device_data:
            target_temp = device_data['acTemperature']
            print(f"Установлена целевая температура: {target_temp}°C")

            if not self.temperature_monitoring:
                self.temperature_monitoring = True
                threading.Thread(target=self.monitor_temperature,
                               args=(target_temp, device_data.get('acMode', 'HEAT'))).start()

    def monitor_temperature(self, target_temp, mode):
        """Мониторинг и имитация регулировки температуры"""
        print(f"Начало мониторинга температуры. Цель: {target_temp}°C, Режим: {mode}")

        while self.temperature_monitoring:
            if mode == "HEAT" and self.current_temp < target_temp:
                self.current_temp += 0.5
                print(f"Нагрев... Текущая температура: {self.current_temp}°C")
            elif mode == "COOL" and self.current_temp > target_temp:
                self.current_temp -= 0.5
                print(f"Охлаждение... Текущая температура: {self.current_temp}°C")

            time.sleep(2)

            if abs(self.current_temp - target_temp) < 0.5:
                print(f"Целевая температура достигнута: {self.current_temp}°C")
                self.temperature_monitoring = False
                break

    def read_sensor_data(self, device_data):
        """Чтение данных с датчика"""
        pins = self.device_pins.get("SENSOR")
        if not pins:
            print("Конфигурация пинов для датчика не найдена")
            return

        sensor_value = GPIO.input(pins["input_pin"])


        device_data['sensor_value'] = sensor_value
        device_data['timestamp'] = datetime.now().isoformat()

        print(f"Данные с датчика {device_data['name']}: {sensor_value}")

    def control_camera(self, device_data):
        """Управление камерой безопасности"""
        is_active = device_data['isActive']

        if is_active:
            print("Камера безопасности активирована")
        else:
            print("Камера безопасности деактивирована")

    def cleanup(self):
        """Очистка ресурсов GPIO"""
        print("Очистка ресурсов GPIO...")

        for pwm_name, pwm_obj in self.pwm_objects.items():
            pwm_obj.stop()

        GPIO.cleanup()
        print("GPIO очищен")

    def start_server(self):
        """Запуск сервера для приема данных"""
        try:
            client_socket, address = self.server_socket.accept()
            print(f"Подключено с адреса {address}")

            while True:
                try:
                    data = client_socket.recv(1024)
                    if not data:
                        break

                    print(f"Получено байт: {len(data)}")

                    device_json = self.parse_device_data(data)

                    if device_json:
                        json_str = json.dumps(device_json, indent=2, ensure_ascii=False)
                        print("Полученные данные устройства:")
                        print(json_str)

                        response_data = {
                            "status": "OK",
                            "message": "Данные получены и обработаны",
                            "gpio_status": "Успешно",
                            "timestamp": datetime.now().isoformat()
                        }

                        response = json.dumps(response_data)
                        client_socket.send(response.encode('utf-8'))
                    else:
                        print("Не удалось распарсить данные")
                        client_socket.send(b"ERROR: Invalid data format")

                except bluetooth.btcommon.BluetoothError as e:
                    print(f"Ошибка Bluetooth: {e}")
                    break
                except Exception as e:
                    print(f"Ошибка: {e}")
                    break

        except KeyboardInterrupt:
            print("\nСервер остановлен пользователем")
        finally:
            client_socket.close()
            self.server_socket.close()
            self.cleanup()
            print("Соединение закрыто")

def main():
    receiver = DeviceDataReceiver(port=1)
    try:
        receiver.start_server()
    except Exception as e:
        print(f"Критическая ошибка: {e}")
        receiver.cleanup()

if __name__ == "__main__":
    main()