# Низкоуровневая обёртка Trans2Quik.dll: как пользоваться

Это руководство к JNA-обёртке `org.jtrans2quik` из исходного проекта
[Enfernuz/JavaTrans2Quik](https://github.com/Enfernuz/JavaTrans2Quik) (автор Арсентий Нерушев).
Текст взят из README оригинала, поправлено форматирование, ссылки на PDF-руководство QUIK заменены
ссылками на сайт ARQA, примеры приведены к JNA 5 и SLF4J. Если нужен готовый сервис с переподключением
и потоком заявок и сделок, смотрите [README](../README.md) и пакет `io.github.soloyes.quikbridge`.

Обёртка писалась под Trans2Quik.dll версии 1.2.0.3. Описание API (раздел «Работа с другими приложениями»,
пункт про импорт транзакций через Trans2QUIK.dll и формат строки транзакции) входит в документацию
к терминалу QUIK; её можно взять на [сайте ARQA](https://arqatech.com/ru/support/files/).

## 1. Загрузка библиотеки

Экземпляр обёртки даёт класс `Trans2QuikLibraryLoader`:

```java
Trans2QuikLibrary quik = Trans2QuikLibraryLoader.LIBRARY;
```

Отдельную ссылку заводить не обязательно, можно обращаться прямо к `Trans2QuikLibraryLoader.LIBRARY`.
DLL загружается при первом обращении к классу, поэтому `TRANS2QUIK.dll` должна лежать в `PATH`
или в папке, указанной через `-Djna.library.path=...`. Разрядность DLL должна совпадать с разрядностью JVM.

## 2. Код ошибки и буфер под сообщение

Большинство функций Trans2Quik.dll возвращают статус выполнения, а при ошибке записывают подробности
в переданные аргументы: ссылку на расширенный код ошибки и буфер под текст. Выделим под них место:

```java
NativeLongByReference errorCode = new NativeLongByReference();
byte[] buffer = new byte[256];
```

Текст ошибки приходит в кодировке windows-1251 и заканчивается нулевым байтом. Читать его нужно так:

```java
String message = CStrings.decode(buffer);   // io.github.soloyes.quikbridge.api.CStrings
```

## 3. Соединение с терминалом

Вызываем `TRANS2QUIK_CONNECT` и передаём путь к папке терминала (где лежит `info.exe`).
В терминале должен быть запущен сервис внешних транзакций.

```java
String pathToQuik = "C:\\Programs\\Quik";
NativeLong rc = quik.TRANS2QUIK_CONNECT(pathToQuik, errorCode, buffer, buffer.length);
```

## 4. Отправка транзакции

1. Составьте строку транзакции. Формат описан в руководстве QUIK (раздел про формат файла транзакций).
2. Вызовите `TRANS2QUIK_SEND_SYNC_TRANSACTION(...)` и передайте строку, объекты под код ответа,
   номер транзакции, номер заявки и сообщения.

Есть и асинхронный способ: `TRANS2QUIK_SEND_ASYNC_TRANSACTION` плюс коллбэк `TransactionReplyCallback`,
который ставится через `TRANS2QUIK_SET_TRANSACTIONS_REPLY_CALLBACK`.

**Внимание.** Строка ниже взята из примера в руководстве к trans2quik.dll. На сервер она не пройдёт:
в ней недействительные значения параметров. Подставьте свой счёт, код клиента и инструмент.

```java
String transaction = "ACCOUNT=NL0080000043; CLIENT_CODE=467; TYPE=L; TRANS_ID=1; "
        + "CLASSCODE=TQBR; SECCODE=RU0008943394; ACTION=NEW_ORDER; OPERATION=S; PRICE=43,21; QUANTITY=3;";

NativeLongByReference replyCode = new NativeLongByReference();
IntByReference transId = new IntByReference();
DoubleByReference orderNum = new DoubleByReference();
byte[] resultMessage = new byte[256];

NativeLong rc = quik.TRANS2QUIK_SEND_SYNC_TRANSACTION(
        transaction, replyCode, transId, orderNum,
        resultMessage, resultMessage.length,
        errorCode, buffer, buffer.length);
```

Коды возврата:

| Код | Константа |
|----:|-----------|
| 0 | TRANS2QUIK_SUCCESS |
| 1 | TRANS2QUIK_FAILED |
| 2 | TRANS2QUIK_QUIK_TERMINAL_NOT_FOUND |
| 3 | TRANS2QUIK_DLL_VERSION_NOT_SUPPORTED |
| 4 | TRANS2QUIK_ALREADY_CONNECTED_TO_QUIK |
| 5 | TRANS2QUIK_WRONG_SYNTAX |
| 6 | TRANS2QUIK_QUIK_NOT_CONNECTED |
| 7 | TRANS2QUIK_DLL_NOT_CONNECTED |
| 8 | TRANS2QUIK_QUIK_CONNECTED |
| 9 | TRANS2QUIK_QUIK_DISCONNECTED |
| 10 | TRANS2QUIK_DLL_CONNECTED |
| 11 | TRANS2QUIK_DLL_DISCONNECTED |
| 12 | TRANS2QUIK_MEMORY_ALLOCATION_ERROR |
| 13 | TRANS2QUIK_WRONG_CONNECTION_HANDLE |
| 14 | TRANS2QUIK_WRONG_INPUT_PARAMS |

Так же вызываются и остальные функции. В Java эти коды разбирает `io.github.soloyes.quikbridge.api.Status`.

## 5. Функции обратного вызова (callbacks)

Терминал сам сообщает о событиях: обрыв связи, ответ на асинхронную транзакцию, новая заявка или сделка.
Для этого DLL вызывает функцию, которую вы ей передали. В обёртке каждый коллбэк — интерфейс-наследник
`StdCallCallback` со своим методом `callback(...)`.

Пример: логируем изменения статуса соединения (`TRANS2QUIK_CONNECTION_STATUS_CALLBACK`, в обёртке
`ConnectionStatusCallback`):

```java
Trans2QuikLibrary.ConnectionStatusCallback connectionStatusCallback =
        (nConnectionEvent, nExtendedErrorCode, lpcstrInfoMessage) ->
                log.info("ConnectionStatusCallback: event={}, ext={}, message={}",
                        nConnectionEvent.intValue(), nExtendedErrorCode.intValue(), lpcstrInfoMessage);
```

Пример: изменение статуса заявки (`OrderStatusCallback`):

```java
Trans2QuikLibrary.OrderStatusCallback orderStatusCallback =
        (nMode, dwTransID, dNumber, classCode, secCode, dPrice,
         nBalance, dValue, nIsSell, nStatus, nOrderDescriptor) ->
                log.info("Order {} {}:{} price={} left={} status={}",
                        (long) dNumber, classCode, secCode, dPrice, nBalance.intValue(), nStatus.intValue());
```

Для сделок в форке добавлен `TradeStatusCallback` (в оригинале у `TRANS2QUIK_START_TRADES` не было
типизированного коллбэка).

Логирование — не единственный вариант. Данные `OrderStatusCallback` можно, например, использовать
для контроля работы робота.

Каждый коллбэк передаётся DLL своей функцией. Для статуса соединения это
`TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK`:

```java
rc = quik.TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK(connectionStatusCallback, errorCode, buffer, buffer.length);
```

**Важно.** Храните объект коллбэка в поле, пока он установлен. Если на него ссылается только DLL,
сборщик мусора может его удалить, и следующий вызов из QUIK уронит процесс.

Какие коллбэки ставить, решаете вы, исходя из нужных событий. Итог: результаты обработки транзакций,
изменения статусов заявок, сделки и состояние соединения приходят через коллбэки.

## 6. Заявки и сделки по подписке

```java
quik.TRANS2QUIK_SUBSCRIBE_ORDERS("TQBR", "SBER|LKOH");   // пустой класс = все классы, пустой список = все бумаги
quik.TRANS2QUIK_START_ORDERS(orderStatusCallback);
// ...
quik.TRANS2QUIK_UNSUBSCRIBE_ORDERS();
```

Это заявки и сделки вашего счёта, не рыночные данные: стакана и котировок в Trans2Quik нет.

## 7. Как получать рыночные данные из QUIK в Java

Автор оригинала предлагал два пути:

1. RPC-сервис на стороне QUIK: [quik-lua-rpc](https://github.com/Enfernuz/quik-lua-rpc) и Java-клиент
   [quik-lua-rpc-java-client](https://github.com/Enfernuz/quik-lua-rpc-java-client). Реализует все методы
   QLua, включая `sendTransaction`, так что с ним `JavaTrans2Quik` уже не нужен.
2. Ещё один RPC-сервис, проще в установке, но беднее: [quik_pipe](https://github.com/boscogh/quik_pipe).
