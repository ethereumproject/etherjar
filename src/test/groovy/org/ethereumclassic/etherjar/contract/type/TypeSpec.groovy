package org.ethereumclassic.etherjar.contract.type

import spock.lang.Specification

import java.util.function.BiFunction
import java.util.function.Function

class TypeSpec extends Specification {

    def "should find an appropriate type"() {
        def type = Stub Type

        Type.Repository repo = { -> [{ Optional.of type } as Function] }

        when:
        def opt = repo.search '_'

        then:
        opt.present
        opt.get() == type
    }

    def "should not find a type"() {
        Type.Repository repo = { -> [] }

        when:
        def opt = repo.search '_'

        then:
        !opt.present
    }

    def "should catch null type string representation"() {
        Type.Repository repo = { -> [] }

        when:
        repo.search null

        then:
        thrown NullPointerException
    }

    def "should catch empty type string representation"() {
        Type.Repository repo = { -> [] }

        when:
        repo.search ''

        then:
        thrown IllegalArgumentException
    }

    def "should append an ordinal type parser"() {
        def parser1 = Stub Function
        def parser2 = Stub Function

        Type.Repository repo1 = { -> [parser1] }

        when:
        def repo2 = repo1.append parser2

        then:
        repo1.typeParsers.size() == 1
        repo1.typeParsers[0] == parser1

        and:
        repo2.typeParsers.size() == 2
        repo2.typeParsers[0] == parser1
        repo2.typeParsers[1] == parser2
    }

    def "should append a recursive type parser"() {
        def parser1 = Stub Function
        def parser2 = Mock BiFunction

        Type.Repository repo1 = { -> [parser1] }

        when:
        def repo2 = repo1.append parser2

        then:
        repo1.typeParsers.size() == 1
        repo1.typeParsers[0] == parser1

        and:
        repo2.typeParsers.size() == 2
        repo2.typeParsers[0] == parser1

        when:
        repo2.typeParsers[1].apply '123'

        then:
        1 * parser2.apply({ it.getTypeParsers().size() == 2 }, '123')
    }

    def "should encode & decode a dynamic type length"() {
        when:
        def data = DynamicType.encodeLength val
        def res = DynamicType.decodeLength data

        then:
        data.toHex() == hex
        res == val

        where:
        val                                                                                     | hex
        1                                                                                       | '0x0000000000000000000000000000000000000000000000000000000000000001'
        12                                                                                      | '0x000000000000000000000000000000000000000000000000000000000000000c'
        123                                                                                     | '0x000000000000000000000000000000000000000000000000000000000000007b'
        123456789                                                                               | '0x00000000000000000000000000000000000000000000000000000000075bcd15'
        Integer.MAX_VALUE                                                                       | '0x000000000000000000000000000000000000000000000000000000007fffffff'
        Long.MAX_VALUE                                                                          | '0x0000000000000000000000000000000000000000000000007fffffffffffffff'
        new BigInteger('+80000000000000000000000000000', 16)                                    | '0x0000000000000000000000000000000000080000000000000000000000000000'
        new BigInteger('+8000000000000000000000000000000000000000000000000000000000000000', 16) | '0x8000000000000000000000000000000000000000000000000000000000000000'
        new BigInteger('+ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', 16) | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff'
    }

    def "should catch negative length before encoding"() {
        when:
        DynamicType.encodeLength val

        then:
        thrown IllegalArgumentException

        where:
        _ | val
        _ | -1
        _ | -123
        _ | Integer.MIN_VALUE
        _ | Long.MIN_VALUE
        _ | new BigInteger('-80000000000000000000000000000', 16)
        _ | new BigInteger('-8000000000000000000000000000000000000000000000000000000000000000', 16)
        _ | new BigInteger('-ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', 16)
    }

    def "should oppose static property to dynamic property"() {
        def t = { flag } as Type

        expect:
        t.static == !t.dynamic

        where:
        _ | flag
        _ | false
        _ | true
    }
}
