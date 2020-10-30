package com.skfo763.rtc.data

const val HANGUP = "hangup"
const val FINISHED = "finished"
const val TIMEOUT = "timeout"

const val OFFER = "offer"
const val ANSWER = "answer"
const val CANDIDATE = "candidate"

// IceCandidate 에서 주고받을 데이터 키값
const val TYPE = "type"
const val SDP = "sdp"
const val ID = "id"
const val LABEL = "label"

// UserJoinInfo 주고받을 키값
const val TOKEN = "token"
const val PASSWORD = "password"
const val SKIN = "skin"

// terminate, disconnect 상태에서 받을 데이터 키값
const val TERMINATED_CASE = "terminationCase"
const val SERVER_DISCONNECT = "io server disconnect"
