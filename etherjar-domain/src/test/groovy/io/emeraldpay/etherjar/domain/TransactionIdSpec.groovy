/*
 * Copyright (c) 2020 EmeraldPay Inc, All Rights Reserved.
 * Copyright (c) 2016-2017 Infinitape Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.emeraldpay.etherjar.domain

import spock.lang.Specification

class TransactionIdSpec extends Specification {

    def "Parse tx id"() {
        expect:
        TransactionId.from(hex).toString() == hex.toLowerCase()
        where:
        hex << [
            '0x99d94ccf4f1ad255ba6538ad53c31cf3a9c49065c9b5822533b0abb5af171d82',
            '0xb8b54c779d2eb83b14bd56875c063064937593871658ae559596a25ea5bc0f91',
            '0xf4457d9466b7a445198ca95781032ff46eebeae71578b9f97c8df1caa7ef9b85',
            '0x0f4f762709c13a6d5253c794f77c2a467384023874418ca1df4cd80ffe651236',
            '0xa009852beaafe46df94f28116491f3f63a1c03567b0a85e97494c2fd95a5ac45',
            '0x0000000000000000000000000000000000000000000000000000000000000000',
            '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff',
            '0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF',
        ]
    }

    def "Fail for invalid value"() {
        when:
        TransactionId.from([0, 1, 2] as byte[])
        then:
        thrown(IllegalArgumentException)

        when:
        TransactionId.from('0x')
        then:
        thrown(IllegalArgumentException)

        when:
        TransactionId.from('0x0')
        then:
        thrown(IllegalArgumentException)

        when:
        TransactionId.from('0xfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff')
        then:
        thrown(IllegalArgumentException)
    }

    def "Equals"() {
        when:
        def act = TransactionId.from("0x0f4f762709c13a6d5253c794f77c2a467384023874418ca1df4cd80ffe651236")
            .equals(TransactionId.from("0x0f4f762709c13a6d5253c794f77c2a467384023874418ca1df4cd80ffe651236"))
        then:
        act

        when:
        act = TransactionId.empty().equals(TransactionId.empty())
        then:
        act

        when:
        act = TransactionId.empty().equals(TransactionId.from("0x0f4f762709c13a6d5253c794f77c2a467384023874418ca1df4cd80ffe651236"))
        then:
        !act

        when:
        act = TransactionId.from("0x0f4f762709c13a6d5253c794f77c2a467384023874418ca1df4cd80ffe651236").equals(TransactionId.empty())
        then:
        !act

        when:
        act = TransactionId.from("0x0f4f762709c13a6d5253c794f77c2a467384023874418ca1df4cd80ffe651236")
            .equals(TransactionId.from("0x77c2a467384023874418ca1df4cd80ffe6512360f4f762709c13a6d5253c794f"))
        then:
        !act
    }
}
