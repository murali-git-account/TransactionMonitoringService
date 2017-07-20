# TransactionMonitoringService
InMemoryTransactionRecorder.java  -> Is the In-memory storage that records transactions
TransactionMonitoringTestApp.java -> A test application that invoke the low-level (non-rest) API to record transactions
                                     and retrieve a statistics
                                     This application is not a replacement for the unit test
tst/*                             -> Contains unit tests to verify the behavior of the classes implemented 