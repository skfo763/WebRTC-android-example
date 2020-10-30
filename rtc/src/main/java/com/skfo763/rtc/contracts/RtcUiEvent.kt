package com.skfo763.rtc.contracts

enum class RtcUiEvent {
    START_CALL,             // 통화 시작
    STOP_PROCESS_COMPLETE,  // 통화 종료
    LAZY_NETWORK,           // 네트워크 지연상태가 되면 이걸 호출해줍니다
    NORMAL_NETWORK,         // 추후 네트워크 지연상태가 해결되면 이걸 호출해줍니다.
    TERMINATE,              // 상대방이 나갔을 때
    TIMEOUT,                // 시간초과로 인한 종료
    FINISH,                 // 에러 등 완전히 종료해줘야할 때
    RETRY                   // 재연결 시도
}