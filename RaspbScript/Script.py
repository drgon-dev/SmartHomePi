# server.py на Raspberry Pi для приема "Hello world"
import bluetooth
import time

def start_bluetooth_server():
    # Создаем Bluetooth сервер
    server_socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    port = 1

    server_socket.bind(("", port))
    server_socket.listen(1)

    print(f"Сервер запущен. Ожидание подключения на порту {port}...")
    print(f"MAC-адрес этого устройства: {bluetooth.read_local_bdaddr()[0]}")

    try:
        while True:
            print("Ожидание подключения...")
            client_socket, client_address = server_socket.accept()
            print(f"Подключение установлено с {client_address}")

            try:
                while True:
                    # Принимаем данные
                    data = client_socket.recv(1024)
                    if not data:
                        break

                    # Декодируем и выводим сообщение
                    message = data.decode('utf-8').strip()
                    print(f"Получено сообщение: {message}")

                    # Проверяем, является ли сообщение "Hello world"
                    if message == "Hello world":
                        print("✓ Получено тестовое сообщение 'Hello world'")
                        # Можно добавить обработку - например, включить светодиод

                    # Отправляем подтверждение обратно
                    response = f"RPi получил: {message}"
                    client_socket.send(response.encode('utf-8'))

            except Exception as e:
                print(f"Ошибка при обработке соединения: {e}")

            finally:
                client_socket.close()
                print("Соединение закрыто")

    except KeyboardInterrupt:
        print("\nСервер остановлен")
    finally:
        server_socket.close()

if __name__ == "__main__":
    start_bluetooth_server()