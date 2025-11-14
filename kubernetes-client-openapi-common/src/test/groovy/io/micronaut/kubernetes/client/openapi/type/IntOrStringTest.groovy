package io.micronaut.kubernetes.client.openapi.type

import spock.lang.Specification

class IntOrStringTest extends Specification {

    def 'test equals'() {
        when:
        def item1 = new IntOrString(2000)
        def item2 = new IntOrString("2000")
        def item3 = new IntOrString(2000)
        def item4 = new IntOrString("2000")

        then:
        item1 != item2
        item3 != item4
        item1 == item3
        item2 == item4
    }

    def 'test hash code'() {
        given:
        def item1 = new IntOrString(2000)
        def item2 = new IntOrString("2000")
        def item3 = new IntOrString(2000)
        def item4 = new IntOrString("2000")

        when:
        def hashCode1 = item1.hashCode()
        def hashCode2 = item2.hashCode()
        def hashCode3 = item3.hashCode()
        def hashCode4 = item4.hashCode()

        then:
        hashCode1 != hashCode2
        hashCode3 != hashCode4
        hashCode1 == hashCode3
        hashCode2 == hashCode4
    }

    def 'test get string value when integer used'() {
        given:
        def item = new IntOrString(2000)

        when:
        item.getStrValue()

        then:
        final IllegalStateException exception = thrown()
        exception.message == 'Not a string'
    }

    def 'test get integer value when string used'() {
        given:
        def item = new IntOrString("2000")

        when:
        item.getIntValue()

        then:
        final IllegalStateException exception = thrown()
        exception.message == 'Not an integer'
    }
}
