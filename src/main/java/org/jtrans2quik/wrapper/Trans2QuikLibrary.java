package org.jtrans2quik.wrapper;

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import com.sun.jna.win32.StdCallLibrary;

/**
 *
 * @author Arsentii Nerushev
 * @version 1.0.1
 */
public interface Trans2QuikLibrary extends StdCallLibrary {
    
    /*
    * Java Native Access (JNA) overview: 
    * https://github.com/java-native-access/jna
    */

    /**
     * @since 1.0.0
     */
    public interface OrderStatusCallback extends StdCallCallback {
        /**
         * typedef void (__stdcall *TRANS2QUIK_ORDER_STATUS_CALLBACK) 
         * (long nMode, DWORD dwTransID, double dNumber, LPCSTR ClassCode, 
         * LPCSTR SecCode, double dPrice, long nBalance, double dValue, 
         * long nIsSell, long nStatus, long nOrderDescriptor);
         * @param nMode
         * @param dwTransID
         * @param dNumber
         * @param classCode
         * @param secCode
         * @param dPrice
         * @param nBalance
         * @param dValue
         * @param nIsSell
         * @param nStatus
         * @param nOrderDescriptor
         * @since 1.0.0
         */
        void callback(
                NativeLong nMode,
                int dwTransID,
                double dNumber,
                String classCode,
                String secCode,
                double dPrice,
                NativeLong nBalance,
                double dValue,
                NativeLong nIsSell,
                NativeLong nStatus,
                NativeLong nOrderDescriptor
        );
    }

    /**
     * Added in the soloyes fork: upstream declared TRANS2QUIK_START_TRADES but had no typed callback for it.
     * Signature as in trans2quik_api.h shipped with Trans2QuikAPI 1.2.
     *
     * typedef void (__stdcall *TRANS2QUIK_TRADE_STATUS_CALLBACK)
     * (long nMode, double dNumber, double dOrderNumber, LPCSTR ClassCode,
     * LPCSTR SecCode, double dPrice, long nQty, double dValue,
     * long nIsSell, long nTradeDescriptor);
     */
    public interface TradeStatusCallback extends StdCallCallback {
        void callback(
                NativeLong nMode,
                double dNumber,
                double dOrderNumber,
                String classCode,
                String secCode,
                double dPrice,
                NativeLong nQty,
                double dValue,
                NativeLong nIsSell,
                NativeLong nTradeDescriptor
        );
    }

    /**
     * @since 1.0.0
     */
    public interface TransactionReplyCallback extends StdCallCallback {
        /**
         * typedef void (__stdcall *TRANS2QUIK_TRANSACTION_REPLY_CALLBACK) 
         * (long nTransactionResult, long nTransactionExtendedErrorCode, 
         * long nTransactionReplyCode, DWORD dwTransId, double dOrderNum, 
         * LPCSTR lpcstrTransactionReplyMessage);
         * @param nTransactionResult
         * @param nTransactionExtendedErrorCode
         * @param nTransactionReplyCode
         * @param dwTransId
         * @param dOrderNum
         * @param lpcstrTransactionReplyMessage
         * @since 1.0.0
         */
        void callback(
                NativeLong nTransactionResult,
                NativeLong nTransactionExtendedErrorCode,
                NativeLong nTransactionReplyCode,
                int dwTransId,
                double dOrderNum,
                String lpcstrTransactionReplyMessage
        );
    }

    /**
     * @since 1.0.0
     */
    public interface ConnectionStatusCallback extends StdCallCallback {
        /**
         * typedef void (__stdcall *TRANS2QUIK_CONNECTION_STATUS_CALLBACK) 
         * (long nConnectionEvent, long nExtendedErrorCode, LPCSTR lpcstrInfoMessage);
         * @param nConnectionEvent
         * @param nExtendedErrorCode
         * @param lpcstrInfoMessage
         * @since 1.0.0
        */
        void callback(
                NativeLong nConnectionEvent,
                NativeLong nExtendedErrorCode,
                String lpcstrInfoMessage
        );
    }
    
    /**
     * Connects the Trans2QUIK.dll library to the QUIK workstation.
     * 
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_CONNECT (LPSTR
     * lpstConnectionParamsString, long* pnExtendedErrorCode, LPSTR
     * lpstrErrorMessage, DWORD dwErrorMessageSize);
     * 
     * @param lpcstrConnectionParamsString Full path to the folder with the INFO.EXE executable to connect to
     * @param pnExtendedErrorCode On error, may contain the extended error code
     * @param lpstrErrorMessage On error, may receive the error message
     * @param errorMessageSize Length of the buffer that lpstrErrorMessage points to
     * @return One of the following values:
        TRANS2QUIK_SUCCESS: the connection is established;
        TRANS2QUIK_QUIK_TERMINAL_NOT_FOUND: there is no INFO.EXE in the given folder, or its external connections service is not running; pnExtendedErrorCode is 0;
        TRANS2QUIK_DLL_VERSION_NOT_SUPPORTED: this INFO.EXE does not support the Trans2QUIK.dll version in use; pnExtendedErrorCode is 0;
        TRANS2QUIK_DLL_ALREADY_CONNECTED_TO_QUIK: the connection is already established; pnExtendedErrorCode is 0;
        TRANS2QUIK_FAILED: connecting failed; pnExtendedErrorCode holds an additional error code.
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_CONNECT(
            String lpcstrConnectionParamsString,
            NativeLongByReference pnExtendedErrorCode,
            byte[] lpstrErrorMessage,
            int errorMessageSize
    );
    
    /**
     * Disconnects the Trans2QUIK.dll library from the QUIK terminal.
     * 
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_DISCONNECT 
     * (long* pnExtendedErrorCode, LPSTR lpstrErrorMessage, DWORD dwErrorMessageSize);
     * @param pnExtendedErrorCode On error, may contain the extended error code
     * @param lpstrErrorMessage On error, may receive the error message
     * @param dwErrorMessageSize Length of the buffer that lpstrErrorMessage points to
     * @return One of the following values:
        TRANS2QUIK_SUCCESS: the library is disconnected from the QUIK workstation;
        TRANS2QUIK_FAILED: disconnecting failed; pnExtendedErrorCode holds an additional error code;
        TRANS2QUIK_DLL_NOT_CONNECTED: there was no connection to break; pnExtendedErrorCode may hold an additional error code.
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_DISCONNECT(
            NativeLongByReference pnExtendedErrorCode,
            byte[] lpstrErrorMessage,
            int dwErrorMessageSize
    );
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_IS_QUIK_CONNECTED 
     * (long* pnExtendedErrorCode, LPSTR lpstrErrorMessage, DWORD dwErrorMessageSize);
     * @param pnExtendedErrorCode
     * @param lpstrErrorMessage
     * @param dwErrorMessageSize
     * @return terminal connection status
     * @since 1.0.0
     */
    public NativeLong TRANS2QUIK_IS_QUIK_CONNECTED(
            NativeLongByReference pnExtendedErrorCode,
            byte[] lpstrErrorMessage,
            int dwErrorMessageSize
    );
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_IS_DLL_CONNECTED 
     * (long* pnExtendedErrorCode, LPSTR lpstrErrorMessage, DWORD dwErrorMessageSize);
     * @param pnExtendedErrorCode
     * @param lpstrErrorMessage
     * @param dwErrorMessageSize
     * @return DLL connection status
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_IS_DLL_CONNECTED(
            NativeLongByReference pnExtendedErrorCode,
            byte[] lpstrErrorMessage,
            int dwErrorMessageSize
    );
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_SEND_SYNC_TRANSACTION (LPSTR lpstTransactionString, 
     * long* pnReplyCode, PDWORD pdwTransId, double* pdOrderNum, LPSTR lpstrResultMessage, 
     * DWORD dwResultMessageSize, long* pnExtendedErrorCode, LPSTR lpstErrorMessage, DWORD dwErrorMessageSize);
     * @param lpstTransactionString
     * @param pnReplyCode
     * @param pdwTransId
     * @param pdOrderNum
     * @param lpstrResultMessage
     * @param dwResultMessageSize
     * @param pnExtendedErrorCode
     * @param lpstrErrorMessage
     * @param dwErrorMessageSize
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_SEND_SYNC_TRANSACTION(
            String lpstTransactionString,
            NativeLongByReference pnReplyCode,
            IntByReference pdwTransId,
            DoubleByReference pdOrderNum,
            byte[] lpstrResultMessage,
            int dwResultMessageSize,
            NativeLongByReference pnExtendedErrorCode,
            byte[] lpstrErrorMessage,
            int dwErrorMessageSize
    );
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_SEND_ASYNC_TRANSACTION (LPSTR lpstTransactionString, 
     * long* pnExtendedErrorCode, LPSTR lpstErrorMessage, DWORD dwErrorMessageSize);
     * @param lpstTransactionString
     * @param pnExtendedErrorCode
     * @param lpstrErrorMessage
     * @param dwErrorMessageSize
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_SEND_ASYNC_TRANSACTION(
            String lpstTransactionString,
            NativeLongByReference pnExtendedErrorCode,
            byte[] lpstrErrorMessage,
            int dwErrorMessageSize
    );

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK 
     * (TRANS2QUIK_CONNECTION_STATUS_CALLBACK pfConnectionStatusCallback, long* pnExtendedErrorCode, 
     * LPSTR lpstrErrorMessage, DWORD dwErrorMessageSize);
     * @param pfConnectionStatusCallback
     * @param pnExtendedErrorCode
     * @param lpstrErrorMessage
     * @param dwErrorMessageSize
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK(
            Callback pfConnectionStatusCallback,
            NativeLongByReference pnExtendedErrorCode,
            byte[] lpstrErrorMessage,
            int dwErrorMessageSize
    );
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_SET_TRANSACTIONS_REPLY_CALLBACK 
     * (TRANS2QUIK_TRANSACTION_REPLY_CALLBACK pfTransactionReplyCallback, long* pnExtendedErrorCode, 
     * LPSTR lpstrErrorMessage, DWORD dwErrorMessageSize);
     * @param pfTransactionReplyCallback
     * @param pnExtendedErrorCode
     * @param lpstrErrorMessage
     * @param dwErrorMessageSize
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_SET_TRANSACTIONS_REPLY_CALLBACK(
            Callback pfTransactionReplyCallback,
            NativeLongByReference pnExtendedErrorCode,
            byte[] lpstrErrorMessage,
            int dwErrorMessageSize
    );
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_SUBSCRIBE_ORDERS (LPSTR
     * ClassCode, LPSTR Seccodes);
     * @param ClassCode
     * @param Seccodes
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_SUBSCRIBE_ORDERS(
            String ClassCode,
            String Seccodes
    );
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_SUBSCRIBE_TRADES (LPSTR ClassCode, LPSTR Seccodes);
     * @param ClassCode
     * @param Seccodes
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_SUBSCRIBE_TRADES(
            String ClassCode,
            String Seccodes
    );

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_START_ORDERS
     * (TRANS2QUIK_ORDER_STATUS_CALLBACK pfnOrderStatusCallback);
     * @param pfnOrderStatusCallback
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_START_ORDERS(Callback pfnOrderStatusCallback);
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_START_TRADES
     * (TRANS2QUIK_TRADE_STATUS_CALLBACK pfnTradeStatusCallback);
     * @param pfnTradeStatusCallback
     * @return 
     */
    NativeLong TRANS2QUIK_START_TRADES(Callback pfnTradeStatusCallback);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_UNSUBSCRIBE_ORDERS ();
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_UNSUBSCRIBE_ORDERS();
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_UNSUBSCRIBE_TRADES ();
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_UNSUBSCRIBE_TRADES();
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_QTY (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_QTY(NativeLong nOrderDescriptor);
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_DATE (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_DATE(NativeLong nOrderDescriptor);
        
    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_TIME (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_TIME(NativeLong nOrderDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_ACTIVATION_TIME (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_ACTIVATION_TIME(NativeLong nOrderDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_WITHDRAW_TIME (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_WITHDRAW_TIME(NativeLong nOrderDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_EXPIRY (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_EXPIRY(NativeLong nOrderDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_ACCRUED_INT (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_ORDER_ACCRUED_INT(NativeLong nOrderDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_YIELD (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_ORDER_YIELD(NativeLong nOrderDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_UID (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_UID(NativeLong nOrderDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_VISIBLE_QTY (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_VISIBLE_QTY(NativeLong nOrderDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_PERIOD (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_PERIOD(NativeLong nOrderDescriptor);
        
    /**
     * FILETIME TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_FILETIME (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return FILETIME structure value
     * @since 1.0.1
     */
    Structure TRANS2QUIK_ORDER_FILETIME(NativeLong nOrderDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_DATE_TIME (long nOrderDescriptor, long nTimeType);
     * @param nOrderDescriptor
     * @param nTimeType
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_ORDER_DATE_TIME(NativeLong nOrderDescriptor, NativeLong nTimeType);

    /**
     * FILETIME TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_WITHDRAW_FILETIME (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return FILETIME structure value
     * @since 1.0.1
     */
    Structure TRANS2QUIK_ORDER_WITHDRAW_FILETIME(NativeLong nOrderDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_USERID (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_ORDER_USERID(NativeLong nOrderDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_ACCOUNT (long nOrderDescriptor); 
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_ORDER_ACCOUNT(NativeLong nOrderDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_BROKERREF (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_ORDER_BROKERREF(NativeLong nOrderDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_CLIENT_CODE (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_ORDER_CLIENT_CODE(NativeLong nOrderDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_ORDER_FIRMID (long nOrderDescriptor);
     * @param nOrderDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_ORDER_FIRMID(NativeLong nOrderDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_DATE (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_DATE(NativeLong nTradeDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_SETTLE_DATE (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_SETTLE_DATE(NativeLong nTradeDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_TIME (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_TIME(NativeLong nTradeDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_IS_MARGINAL (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_IS_MARGINAL(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_ACCRUED_INT (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_ACCRUED_INT(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_YIELD (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_YIELD(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_TS_COMMISSION (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_TS_COMMISSION(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_CLEARING_CENTER_COMMISSION (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_CLEARING_CENTER_COMMISSION(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_EXCHANGE_COMMISSION (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_EXCHANGE_COMMISSION(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_TRADING_SYSTEM_COMMISSION (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_TRADING_SYSTEM_COMMISSION(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_PRICE2 (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_PRICE2(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_REPO_RATE (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_REPO_RATE(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_REPO_VALUE (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_REPO_VALUE(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_REPO2_VALUE (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_REPO2_VALUE(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_ACCRUED_INT2 (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_ACCRUED_INT2(NativeLong nTradeDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_REPO_TERM (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_REPO_TERM(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_START_DISCOUNT (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_START_DISCOUNT(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_LOWER_DISCOUNT (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_LOWER_DISCOUNT(NativeLong nTradeDescriptor);

    /**
     * double TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_UPPER_DISCOUNT (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    double TRANS2QUIK_TRADE_UPPER_DISCOUNT(NativeLong nTradeDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_BLOCK_SECURITIES (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_BLOCK_SECURITIES(NativeLong nTradeDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_PERIOD (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_PERIOD(NativeLong nTradeDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_REPO_TERM (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_KIND(NativeLong nTradeDescriptor);

    /**
     * FILETIME TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_FILETIME (long nTradeDescriptor);
     */
    //void TRANS2QUIK_TRADE_FILETIME(NativeLong nTradeDescriptor);

    /**
     * long TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_DATE_TIME (long nTradeDescriptor, long nTimeType);
     * @param nTradeDescriptor
     * @param nTimeType
     * @return
     * @since 1.0.0
     */
    NativeLong TRANS2QUIK_TRADE_DATE_TIME(NativeLong nTradeDescriptor, NativeLong nTimeType);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_CURRENCY (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_CURRENCY(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_SETTLE_CURRENCY (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_SETTLE_CURRENCY(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_SETTLE_CODE (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_SETTLE_CODE(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_ACCOUNT (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_ACCOUNT(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_BROKERREF (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_BROKERREF(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_CLIENT_CODE (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_CLIENT_CODE(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_USERID (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_USERID(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_FIRMID (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_FIRMID(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_PARTNER_FIRMID (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_PARTNER_FIRMID(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_EXCHANGE_CODE (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_EXCHANGE_CODE(NativeLong nTradeDescriptor);

    /**
     * LPTSTR TRANS2QUIK_API __stdcall TRANS2QUIK_TRADE_STATION_ID (long nTradeDescriptor);
     * @param nTradeDescriptor
     * @return
     * @since 1.0.0
     */
    WString TRANS2QUIK_TRADE_STATION_ID(NativeLong nTradeDescriptor);
}
