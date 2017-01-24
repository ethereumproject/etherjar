package org.ethereumclassic.etherjar.contract.type

import org.ethereumclassic.etherjar.model.Hex32
import spock.lang.Specification

class DecimalTypeTest extends Specification {

    static class DecimalTypeImpl extends DecimalType {
        public minMock, maxMock

        protected DecimalTypeImpl() {
            super(128, 128, false)
        }

        protected DecimalTypeImpl(int bits) {
            super(bits, bits, false)
        }

        protected DecimalTypeImpl(int mBits, int nBits) {
            super(mBits, nBits, false)
        }

        protected DecimalTypeImpl(int mBits, int nBits, boolean signed) {
            super(mBits, nBits, signed)
        }

        protected DecimalTypeImpl (int mBits, int nBits, boolean signed, BigDecimal min, BigDecimal max) {
            super(mBits, nBits, signed)
            minMock = min
            maxMock = max
        }

        @Override
        BigDecimal getMinValue() {
            throw new UnsupportedOperationException()
        }

        @Override
        BigDecimal getMaxValue() {
            throw new UnsupportedOperationException()
        }

        @Override
        String getCanonicalName() {
            throw new UnsupportedOperationException()
        }
    }

    final static DEFAULT = [] as DecimalTypeImpl

    def "should create a correct default instance"() {
        expect:
        DEFAULT.MBits == 128
        DEFAULT.NBits == 128
        DEFAULT.bits == 256
        !DEFAULT.signed
        DEFAULT.static
        DEFAULT.fixedSize == Hex32.SIZE_BYTES
    }

    def "should return a power of two"() {
        def res = DecimalType.powerOfTwo bits

        expect:
        res == pow as BigDecimal

        where:
        bits    | pow
        0       | 1
        1       | 2
        2       | 4
        3       | 8
        8       | 256
        12      | 4096
        128     | 340282366920938463463374607431768211456
        250     | 1809251394333065553493296640760748560207343510400633813116524750123642650624
    }

    def "should create an unsigned instance with specified number of bits"() {
        def obj = [8, 16, false] as DecimalTypeImpl

        expect:
        obj.MBits == 8
        obj.NBits == 16
        obj.bits == 24
        !obj.signed
    }

    def "should create a signed instance with specified number of bits"() {
        def obj = [24, 64, true] as DecimalTypeImpl

        expect:
        obj.MBits == 24
        obj.NBits == 64
        obj.bits == 88
        obj.signed
    }

    def "should prevent from incorrect number of side bits"() {
        when:
        new DecimalTypeImpl(bits, 8)

        then:
        thrown IllegalArgumentException

        when:
        new DecimalTypeImpl(8, bits)

        then:
        thrown IllegalArgumentException

        where:
        _ | bits
        _ | -8
        _ | -1
        _ | 0
        _ | 1
        _ | 7
        _ | 21
        _ | 123
        _ | 137
        _ | 256
    }

    def "should prevent from incorrect number of total bits"() {
        when:
        new DecimalTypeImpl(m, n)

        then:
        thrown IllegalArgumentException

        where:
        m   | n
        128 | 136
        144 | 120
        256 | 256
    }

    def "should encode double values"() {
        def obj = [
                mBits: 128,
                nBits: 128,
                isSigned: true,
                getMinValue: -0x80000000000000000000000000000000 as BigDecimal,
                getMaxValue: 0x80000000000000000000000000000000 as BigDecimal,
        ] as DecimalTypeImpl

        when:
        def data = obj.encode val

        then:
        data.toHex() == hex

        where:
        val     | hex
        0D      | '0x0000000000000000000000000000000000000000000000000000000000000000'
        8.5D    | '0x0000000000000000000000000000000880000000000000000000000000000000'
        2.125D  | '0x0000000000000000000000000000000220000000000000000000000000000000'
    }

    def "should round before encode"() {
        def obj = [
                mBits: 64,
                nBits: 64,
                isSigned: true,
                getMinValue: -0x8000000000000000 as BigDecimal,
                getMaxValue: 0x8000000000000000 as BigDecimal,
        ] as DecimalTypeImpl

        when:
        def data = obj.encode before
        def res = obj.decode data

        then:
        res == after

        where:
        before          | after
        64.123456789    | 64.12345678900000000000000000000000000000108505538897284720685964617300743925099415799757733569474993373660254292190074920654296875
    }

    def "should encode & decode "() {
        def obj = new DecimalTypeImpl(m, n, sign) {
            @Override
            BigDecimal getMinValue() {
                isSigned() ?
                        -0x80000000000000000000000000000000 :
                        0G
            }

            @Override
            BigDecimal getMaxValue() {
                isSigned() ?
                        0x80000000000000000000000000000000 :
                        0x100000000000000000000000000000000
            }
        }

        when:
        def data = obj.encodeStatic(val as BigDecimal)
        def res = obj.decodeStatic data

        then:
        data.toHex() == hex
        res == val as BigDecimal

        where:
        m   | n | sign  | val           | hex
        8   | 8 | true  | -0.5          | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff80'
        8   | 8 | true  | 0.5           | '0x0000000000000000000000000000000000000000000000000000000000000080'
        8   | 8 | true  | -1.0          | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00'
        8   | 8 | true  | 1.0           | '0x0000000000000000000000000000000000000000000000000000000000000100'
        8   | 8 | true  | -1.5          | '0xfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe80'
        8   | 8 | true  | 1.5           | '0x0000000000000000000000000000000000000000000000000000000000000180'
        8   | 8 | true  | -123.625      | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8460'
        8   | 8 | true  | 123.625       | '0x0000000000000000000000000000000000000000000000000000000000007ba0'
        8   | 8 | true  | -0.00390625   | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff'
        8   | 8 | true  | 0.00390625    | '0x0000000000000000000000000000000000000000000000000000000000000001'
        8   | 8 | true  | -128          | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8000'
        8   | 8 | true  | -127.99609375 | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8001'
        8   | 8 | true  | -127.00390625 | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff80ff'
        8   | 8 | true  | 127.00390625  | '0x0000000000000000000000000000000000000000000000000000000000007f01'
        8   | 8 | true  | 127.99609375  | '0x0000000000000000000000000000000000000000000000000000000000007fff'
        8   | 8 | false | 128           | '0x0000000000000000000000000000000000000000000000000000000000008000'
        8   | 8 | false | 211.83203125  | '0x000000000000000000000000000000000000000000000000000000000000d3d5'
        8   | 8 | false | 255.99609375  | '0x000000000000000000000000000000000000000000000000000000000000ffff'

        16  | 16    | true  | -32767.5                  | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffff80008000'
        16  | 16    | true  | 32766.5                   | '0x000000000000000000000000000000000000000000000000000000007ffe8000'
        16  | 16    | true  | -32768                    | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffff80000000'
        16  | 16    | true  | -32767.9999847412109375   | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffff80000001'
        16  | 16    | true  | -32767.0000152587890625   | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffff8000ffff'
        16  | 16    | true  | 32767.0000152587890625    | '0x000000000000000000000000000000000000000000000000000000007fff0001'
        16  | 16    | true  | 32767.9999847412109375    | '0x000000000000000000000000000000000000000000000000000000007fffffff'
        16  | 16    | false | 32768                     | '0x0000000000000000000000000000000000000000000000000000000080000000'
        16  | 16    | false | 65535.9999847412109375    | '0x00000000000000000000000000000000000000000000000000000000ffffffff'-1701-1701

        128 | 128   | true  | -0.5  | '0xffffffffffffffffffffffffffffffff80000000000000000000000000000000'
        128 | 128   | true  | 0.5   | '0x0000000000000000000000000000000080000000000000000000000000000000'
        128 | 128   | true  | -1.5  | '0xfffffffffffffffffffffffffffffffe80000000000000000000000000000000'
        128 | 128   | true  | 1.5   | '0x0000000000000000000000000000000180000000000000000000000000000000'
    }

    def "should catch out of range before encoding"() {
        def obj = new DecimalTypeImpl(m, n, sign, min, max) {
            @Override
            BigDecimal getMinValue() { minMock }

            @Override
            BigDecimal getMaxValue() { maxMock }
        }

        when:
        obj.encodeStatic(val as BigDecimal)

        then:
        thrown IllegalArgumentException

        where:
        m   | n     | sign  | min    | max   | val
        8   | 8     | true  | -128   | 128   | -129
        8   | 8     | true  | -128   | 128   | 128
        8   | 8     | false |  0     | 256   | 256
        16  | 16    | true  | -32768 | 32768 | -32769
        16  | 16    | true  | -32768 | 32768 | 32768
        16  | 16    | false |  0     | 65536 | 65536
        128 | 128   | true  | -0x80000000000000000000000000000000  | 0x80000000000000000000000000000000 | 0x80000000000000000000000000000000
        128 | 128   | false | 0      | 0x100000000000000000000000000000000 | 0x100000000000000000000000000000000
    }

    def "should catch out of range after decoding"() {
        def obj = [m, n, sign] as DecimalTypeImpl

        when:
        obj.decodeStatic(Hex32.from(hex))

        then:
        thrown IllegalArgumentException

        where:
        m   | n | sign  | hex
        8   | 8     | true  | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f00'
        8   | 8     | true  | '0x0000000000000000000000000000000000000000000000000000000000008000'
        8   | 8     | false | '0x0000000000000000000000000000000000000000000000000000000000010000'
        16  | 16    | true  | '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffff7fff0000'
        16  | 16    | true  | '0x0000000000000000000000000000000000000000000000000000000080000000'
        16  | 16    | false | '0x0000000000000000000000000000000000000000000000000000000100000000'
    }

    def "should calculate consistent hashcode"() {
        expect:
        first.hashCode() == second.hashCode()

        where:
        first                               | second
        DEFAULT                        | [] as DecimalTypeImpl
        DEFAULT                        | [128] as DecimalTypeImpl
        [64, 24, true] as DecimalTypeImpl   | [64, 24, true] as DecimalTypeImpl
    }

    def "should be equal"() {
        expect:
        first == second

        where:
        first                               | second
        DEFAULT                        | DEFAULT
        DEFAULT                        | [] as DecimalTypeImpl
        DEFAULT                        | [128] as DecimalTypeImpl
        [64, 24, true] as DecimalTypeImpl   | [64, 24, true] as DecimalTypeImpl
    }

    def "should not be equal"() {
        expect:
        first != second

        where:
        first           | second
        DEFAULT    | null
        DEFAULT    | [64, 24, true] as DecimalTypeImpl
        DEFAULT    | BoolType.DEFAULT
    }

    def "should be converted to a string representation"() {
        def obj = [
                getCanonicalName: 'impl'
        ] as DecimalTypeImpl

        expect:
        obj as String == 'impl'
    }
}
