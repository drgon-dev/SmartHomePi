import json
import struct
import bluetooth

class DeviceDataReceiver:
    def __init__(self, port=1):

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

        print(f"Bluetooth сервер запущен на порту {port}")
        print("Ожидание подключения...")

    def parse_device_data(self, data_bytes):
        try:
            # Структура:
            # 1. Длина ID (1 байт) + ID (строка)
            # 2. Длина имени (1 байт) + имя (строка)
            # 3. Тип устройства (1 байт)
            # 4. Длина статуса (1 байт) + статус (строка)
            # 5. Активность (1 байт)
            # 6. Иконка (4 байта)

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

            return device_data

        except Exception as e:
            print(f"Ошибка при парсинге данных: {e}")
            return None

    def start_server(self):
        """
        Запуск сервера для приема данных
        """
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

                        response = json.dumps({"status": "OK", "message": "Данные получены"})
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
            print("Соединение закрыто")

def main():
    receiver = DeviceDataReceiver(port=1)
    receiver.start_server()

if __name__ == "__main__":
    main()