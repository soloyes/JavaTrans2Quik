# Using the low-level Trans2Quik.dll wrapper

This guide covers the JNA wrapper `org.jtrans2quik` from the original project
[Enfernuz/JavaTrans2Quik](https://github.com/Enfernuz/JavaTrans2Quik) by Arsentii Nerushev.
It is translated from the original README, with the formatting cleaned up, links to the QUIK PDF manual
replaced by links to the ARQA site, and the examples updated to JNA 5 and SLF4J. If you want a ready
service with reconnects and an orders and trades stream, see the [README](../README.md) and the
`io.github.soloyes.quikbridge` package.

The wrapper targets Trans2Quik.dll version 1.2.0.3. The API is described in the QUIK terminal
documentation (the chapter on working with other applications, the part about importing transactions
through Trans2QUIK.dll and the transaction string format). You can get it from the
[ARQA site](https://arqatech.com/ru/support/files/).

## 1. Loading the library

`Trans2QuikLibraryLoader` gives you the wrapper instance:

```java
Trans2QuikLibrary quik = Trans2QuikLibraryLoader.LIBRARY;
```

You don't need your own reference; calling `Trans2QuikLibraryLoader.LIBRARY` directly works too.
The DLL is loaded the first time the class is touched, so `TRANS2QUIK.dll` has to be on `PATH`
or in a folder passed with `-Djna.library.path=...`. The DLL and the JVM must have the same bitness.

## 2. Error code and message buffer

Most Trans2Quik.dll functions return a status code and, on error, write the details into arguments you pass:
a reference for the extended error code and a buffer for the text. Allocate them first:

```java
NativeLongByReference errorCode = new NativeLongByReference();
byte[] buffer = new byte[256];
```

The error text comes in windows-1251 and ends with a zero byte. Read it like this:

```java
String message = CStrings.decode(buffer);   // io.github.soloyes.quikbridge.api.CStrings
```

## 3. Connecting to the terminal

Call `TRANS2QUIK_CONNECT` with the path to the terminal folder (the one with `info.exe`).
The terminal must have the external transactions service running.

```java
String pathToQuik = "C:\\Programs\\Quik";
NativeLong rc = quik.TRANS2QUIK_CONNECT(pathToQuik, errorCode, buffer, buffer.length);
```

## 4. Sending a transaction

1. Build the transaction string. The format is described in the QUIK manual (the section on the transaction file format).
2. Call `TRANS2QUIK_SEND_SYNC_TRANSACTION(...)` with the string and the holders for the reply code,
   transaction id, order number and messages.

There is also an asynchronous way: `TRANS2QUIK_SEND_ASYNC_TRANSACTION` plus a `TransactionReplyCallback`
set with `TRANS2QUIK_SET_TRANSACTIONS_REPLY_CALLBACK`.

The string below comes from the example in the trans2quik.dll manual. The server will reject it because
the parameter values are not real. Put in your own account, client code and instrument.

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

Return codes:

| Code | Constant |
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

The other functions work the same way. In Java, `io.github.soloyes.quikbridge.api.Status` decodes these codes.

## 5. Callbacks

The terminal reports events on its own: a lost connection, the reply to an asynchronous transaction,
a new order or trade. It does this by calling a function you gave the DLL. In the wrapper each callback is
an interface extending `StdCallCallback` with its own `callback(...)` method.

Logging connection status changes (`TRANS2QUIK_CONNECTION_STATUS_CALLBACK`, `ConnectionStatusCallback`
in the wrapper):

```java
Trans2QuikLibrary.ConnectionStatusCallback connectionStatusCallback =
        (nConnectionEvent, nExtendedErrorCode, lpcstrInfoMessage) ->
                log.info("ConnectionStatusCallback: event={}, ext={}, message={}",
                        nConnectionEvent.intValue(), nExtendedErrorCode.intValue(), lpcstrInfoMessage);
```

Order status changes (`OrderStatusCallback`):

```java
Trans2QuikLibrary.OrderStatusCallback orderStatusCallback =
        (nMode, dwTransID, dNumber, classCode, secCode, dPrice,
         nBalance, dValue, nIsSell, nStatus, nOrderDescriptor) ->
                log.info("Order {} {}:{} price={} left={} status={}",
                        (long) dNumber, classCode, secCode, dPrice, nBalance.intValue(), nStatus.intValue());
```

The fork adds `TradeStatusCallback` for trades; upstream had no typed callback for `TRANS2QUIK_START_TRADES`.

You can do more than log. A trading bot can use `OrderStatusCallback` data to keep track of its own orders.

Each callback is handed to the DLL through its own function. For connection status that is
`TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK`:

```java
rc = quik.TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK(connectionStatusCallback, errorCode, buffer, buffer.length);
```

Keep the callback object in a field for as long as it is installed. If only the DLL refers to it,
the garbage collector can collect it, and the next call from QUIK will crash the process.

Which callbacks to install depends on the events you care about. Transaction results, order status
changes, trades and connection state all arrive through callbacks.

## 6. Orders and trades by subscription

```java
quik.TRANS2QUIK_SUBSCRIBE_ORDERS("TQBR", "SBER|LKOH");   // empty class = all classes, empty list = all securities
quik.TRANS2QUIK_START_ORDERS(orderStatusCallback);
// ...
quik.TRANS2QUIK_UNSUBSCRIBE_ORDERS();
```

These are your account's orders and trades. Trans2Quik has no market data: no order book and no quotes.

## 7. Getting market data from QUIK into Java

The original author suggested two options:

1. An RPC service on the QUIK side: [quik-lua-rpc](https://github.com/Enfernuz/quik-lua-rpc) with the Java client
   [quik-lua-rpc-java-client](https://github.com/Enfernuz/quik-lua-rpc-java-client). It implements every QLua
   method, `sendTransaction` included, so with it you no longer need `JavaTrans2Quik`.
2. Another RPC service, easier to install but with fewer features: [quik_pipe](https://github.com/boscogh/quik_pipe).
