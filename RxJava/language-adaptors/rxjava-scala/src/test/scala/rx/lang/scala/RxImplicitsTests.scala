/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala

import org.scalatest.junit.JUnitSuite

/**
 * This is the test suite for the old Scala adaptor.
 */
class OldUnitTestSuite extends JUnitSuite {
    import rx.lang.scala.RxImplicits._

    import org.junit.{ Before, Test }
    import org.junit.Assert._
    import org.mockito.Matchers.any
    import org.mockito.Mockito._
    import org.mockito.{ MockitoAnnotations, Mock }
    import rx.{ Notification, Observer, Observable, Subscription }
    import rx.Observable.OnSubscribeFunc
    import rx.observables.GroupedObservable
    import rx.subscriptions.Subscriptions
    import collection.mutable.ArrayBuffer
    import collection.JavaConverters._
            
    @Mock private[this]
    val observer: Observer[Any] = null
    
    @Mock private[this]
    val subscription: Subscription = null
    
    val isOdd = (i: Int) => i % 2 == 1
    val isEven = (i: Int) => i % 2 == 0
    
    class OnSubscribeWithException(s: Subscription, values: String*) extends OnSubscribeFunc[String] {
        var t: Thread = null
        
        override def onSubscribe(observer: Observer[_ >: String]): Subscription = {
            println("ObservableWithException subscribed to ...")
                    try {
                        println("running ObservableWithException thread")
                        values.toList.foreach(v => {
                            println("ObservableWithException onNext: " + v)
                            observer.onNext(v)
                        })
                        throw new RuntimeException("Forced Failure")
                    } catch {
                        case ex: Exception => observer.onError(ex)
                    }
            s
        }
    }
    
    @Before def before {
        MockitoAnnotations.initMocks(this)
    }
    
    // tests of static methods
    @Test def testSingle {
        assertEquals(1, Observable.from(1).toBlockingObservable.single)
    }
    
    @Test def testSinglePredicate {
        val found = Observable.from(1, 2, 3).toBlockingObservable.single(isEven)
        assertEquals(2, found)
    }
    
    @Test def testSingleOrDefault {
        assertEquals(0, Observable.empty[Int]().toBlockingObservable.singleOrDefault(0))
        assertEquals(1, Observable.from(1).toBlockingObservable.singleOrDefault(0))
        try {
            Observable.from(1, 2, 3).toBlockingObservable.singleOrDefault(0)
            fail("Did not catch any exception, expected IllegalStateException")
        } catch {
            case ex: IllegalStateException => println("Caught expected IllegalStateException")
            case ex: Throwable => fail("Caught unexpected exception " + ex.getCause + ", expected IllegalStateException")
        }
    }
    
    @Test def testSingleOrDefaultPredicate {
        assertEquals(2, Observable.from(1, 2, 3).toBlockingObservable.singleOrDefault(0, isEven))
        assertEquals(0, Observable.from(1, 3).toBlockingObservable.singleOrDefault(0, isEven))
        try {
            Observable.from(1, 2, 3).toBlockingObservable.singleOrDefault(0, isOdd)
            fail("Did not catch any exception, expected IllegalStateException")
        } catch {
            case ex: IllegalStateException => println("Caught expected IllegalStateException")
            case ex: Throwable => fail("Caught unexpected exception " + ex.getCause + ", expected IllegalStateException")
        }
    }

    @Test def testCreateFromOnSubscribeFunc {
        val created = Observable.create((o: Observer[_ >: Integer]) => Subscriptions.empty)
        //no assertions on subscription, just testing the implicit
    }
    
    @Test def testFromJavaInterop {
        val observable = Observable.from(List(1, 2, 3).asJava)
        assertSubscribeReceives(observable)(1, 2, 3)
    }
    
    @Test def testSubscribe {
        val observable = Observable.from("1", "2", "3")
        assertSubscribeReceives(observable)("1", "2", "3")
    }
    
    //should not compile - adapted from https://gist.github.com/jmhofer/5195589
    /*@Test def testSubscribeOnInt() {
        val observable = Observable.from("1", "2", "3")
        observable.subscribe((arg: Int) => {
            println("testSubscribe: arg = " + arg)
        })
     }*/
    
    @Test def testDefer {
        val lazyObservableFactory = () => Observable.from(1, 2)
        val observable = Observable.defer(lazyObservableFactory)
        assertSubscribeReceives(observable)(1, 2)
    }
    
    @Test def testJust {
        val observable = Observable.just("foo")
        assertSubscribeReceives(observable)("foo")
    }
    
    @Test def testMerge {
        val observable1 = Observable.from(1, 2, 3)
        val observable2 = Observable.from(4, 5, 6)
        val merged = Observable.merge(observable1, observable2)
        assertSubscribeReceives(merged)(1, 2, 3, 4, 5, 6)
    }
    
    @Test def testFlattenMerge {
        val observable = Observable.from(Observable.from(1, 2, 3))
        val merged = Observable.merge[Int](observable)
        assertSubscribeReceives(merged)(1, 2, 3)
    }
    
    @Test def testSequenceMerge {
        val observable1 = Observable.from(1, 2, 3)
        val observable2 = Observable.from(4, 5, 6)
        val merged = Observable.merge(observable1, observable2)
        assertSubscribeReceives(merged)(1, 2, 3, 4, 5, 6)
    }
    
    @Test def testConcat {
        val observable1 = Observable.from(1, 2, 3)
        val observable2 = Observable.from(4, 5, 6)
        val concatenated = Observable.concat(observable1, observable2)
        assertSubscribeReceives(concatenated)(1, 2, 3, 4, 5, 6)
    }
    
    @Test def testSynchronize {
        val observable = Observable.from(1, 2, 3)
        val synchronized = Observable.synchronize(observable)
        assertSubscribeReceives(synchronized)(1, 2, 3)
    }
    
    @Test def testZip2() {
        val colors: Observable[String] = Observable.from("red", "green", "blue")
        val names: Observable[String] = Observable.from("lion-o", "cheetara", "panthro")
        
        case class Character(color: String, name: String)
        
        val cheetara = Character("green", "cheetara")
        val panthro = Character("blue", "panthro")
        val characters = Observable.zip[String, String, Character](colors, names, Character.apply _)
        assertSubscribeReceives(characters)(cheetara, panthro)
    }
    
    @Test def testZip3() {
        val numbers = Observable.from(1, 2, 3)
        val colors = Observable.from("red", "green", "blue")
        val names = Observable.from("lion-o", "cheetara", "panthro")
        
        case class Character(id: Int, color: String, name: String)
        
        val liono = Character(1, "red", "lion-o")
        val cheetara = Character(2, "green", "cheetara")
        val panthro = Character(3, "blue", "panthro")
        
        val characters = Observable.zip[Int, String, String, Character](numbers, colors, names, Character.apply _)
        assertSubscribeReceives(characters)(liono, cheetara, panthro)
    }
    
    @Test def testZip4() {
        val numbers = Observable.from(1, 2, 3)
        val colors = Observable.from("red", "green", "blue")
        val names = Observable.from("lion-o", "cheetara", "panthro")
        val isLeader = Observable.from(true, false, false)
        
        case class Character(id: Int, color: String, name: String, isLeader: Boolean)
        
        val liono = Character(1, "red", "lion-o", true)
        val cheetara = Character(2, "green", "cheetara", false)
        val panthro = Character(3, "blue", "panthro", false)
        
        val characters = Observable.zip[Int, String, String, Boolean, Character](numbers, colors, names, isLeader, Character.apply _)
        assertSubscribeReceives(characters)(liono, cheetara, panthro)
    }
    
    //tests of instance methods
    
    // missing tests for : takeUntil, groupBy, next, mostRecent
    
    @Test def testFilter {
        val numbers = Observable.from(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val observable = numbers.filter(isEven)
        assertSubscribeReceives(observable)(2, 4, 6, 8)
    }
    
    @Test def testLast {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(4, observable.toBlockingObservable.last)
    }
    
    @Test def testLastPredicate {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(3, observable.toBlockingObservable.last(isOdd))
    }
    
    @Test def testLastOrDefault {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(4, observable.toBlockingObservable.lastOrDefault(5))
        assertEquals(5, Observable.empty[Int]().toBlockingObservable.lastOrDefault(5))
    }
    
    @Test def testLastOrDefaultPredicate {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(3, observable.toBlockingObservable.lastOrDefault(5, isOdd))
        assertEquals(5, Observable.empty[Int]().toBlockingObservable.lastOrDefault(5, isOdd))
    }
    
    @Test def testMap {
        val numbers = Observable.from(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val mappedNumbers = ArrayBuffer.empty[Int]
        val mapped: Observable[Int] = numbers map ((x: Int) => x * x)
        mapped.subscribe((squareVal: Int) => {
            mappedNumbers.append(squareVal)
        })
        assertEquals(List(1, 4, 9, 16, 25, 36, 49, 64, 81), mappedNumbers.toList)
    }
    
    @Test def testMapMany {
        val numbers = Observable.from(1, 2, 3, 4)
        val f = (i: Int) => Observable.from(List(i, -i).asJava)
        val mappedNumbers = ArrayBuffer.empty[Int]
        numbers.mapMany(f).subscribe((i: Int) => {
            mappedNumbers.append(i)
        })
        assertEquals(List(1, -1, 2, -2, 3, -3, 4, -4), mappedNumbers.toList)
    }
    
    @Test def testMaterialize {
        val observable = Observable.from(1, 2, 3, 4)
        val expectedNotifications: List[Notification[Int]] =
            ((1.to(4).map(i => new Notification(i))) :+ new Notification()).toList
        val actualNotifications: ArrayBuffer[Notification[Int]] = ArrayBuffer.empty
        observable.materialize.subscribe((n: Notification[Int]) => {
            actualNotifications.append(n)
        })
        assertEquals(expectedNotifications, actualNotifications.toList)
    }
    
    @Test def testDematerialize {
        val notifications: List[Notification[Int]] =
            ((1.to(4).map(i => new Notification(i))) :+ new Notification()).toList
        val observableNotifications: Observable[Notification[Int]] =
            Observable.from(notifications.asJava)
        val observable: Observable[Int] =
            observableNotifications.dematerialize()
        assertSubscribeReceives(observable)(1, 2, 3, 4)
    }
    
    @Test def testOnErrorResumeNextObservableNoError {
        val observable = Observable.from(1, 2, 3, 4)
        val resumeObservable = Observable.from(5, 6, 7, 8)
        val observableWithErrorHandler = observable.onErrorResumeNext(resumeObservable)
        assertSubscribeReceives(observableWithErrorHandler)(1, 2, 3, 4)
    }
    
    @Test def testOnErrorResumeNextObservableErrorOccurs {
        val observable = Observable.create(new OnSubscribeWithException(subscription, "foo", "bar"))
        val resumeObservable = Observable.from("a", "b", "c", "d")
        val observableWithErrorHandler = observable.onErrorResumeNext(resumeObservable)
        observableWithErrorHandler.subscribe(observer.asInstanceOf[Observer[String]])
        
        List("foo", "bar", "a", "b", "c", "d").foreach(t => verify(observer, times(1)).onNext(t))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
    }
    
    @Test def testOnErrorResumeNextFuncNoError {
        val observable = Observable.from(1, 2, 3, 4)
        val resumeFunc = (ex: Throwable) => Observable.from(5, 6, 7, 8)
        val observableWithErrorHandler = observable.onErrorResumeNext(resumeFunc)
        assertSubscribeReceives(observableWithErrorHandler)(1, 2, 3, 4)
    }
    
    @Test def testOnErrorResumeNextFuncErrorOccurs {
        val observable = Observable.create(new OnSubscribeWithException(subscription, "foo", "bar"))
        val resumeFunc = (ex: Throwable) => Observable.from("a", "b", "c", "d")
        val observableWithErrorHandler = observable.onErrorResumeNext(resumeFunc)
        observableWithErrorHandler.subscribe(observer.asInstanceOf[Observer[String]])
        
        List("foo", "bar", "a", "b", "c", "d").foreach(t => verify(observer, times(1)).onNext(t))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
    }
    
    @Test def testOnErrorReturnFuncNoError {
        val observable = Observable.from(1, 2, 3, 4)
        val returnFunc = (ex: Throwable) => 87
        val observableWithErrorHandler = observable.onErrorReturn(returnFunc)
        assertSubscribeReceives(observableWithErrorHandler)(1, 2, 3, 4)
    }
    
    @Test def testOnErrorReturnFuncErrorOccurs {
        val observable = Observable.create(new OnSubscribeWithException(subscription, "foo", "bar"))
        val returnFunc = (ex: Throwable) => "baz"
        val observableWithErrorHandler = observable.onErrorReturn(returnFunc)
        observableWithErrorHandler.subscribe(observer.asInstanceOf[Observer[String]])
        
        List("foo", "bar", "baz").foreach(t => verify(observer, times(1)).onNext(t))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
    }
    
    @Test def testReduce {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(10, observable.reduce((a: Int, b: Int) => a + b).toBlockingObservable.single)
    }
    
    @Test def testSkip {
        val observable = Observable.from(1, 2, 3, 4)
        val skipped = observable.skip(2)
        assertSubscribeReceives(skipped)(3, 4)
    }
    
    @Test def testTake {
        val observable = Observable.from(1, 2, 3, 4, 5)
        val took = observable.take(2)
        assertSubscribeReceives(took)(1, 2)
    }
    
    @Test def testTakeWhile {
        val observable = Observable.from(1, 3, 5, 6, 7, 9, 11)
        val took = observable.takeWhile(isOdd)
        assertSubscribeReceives(took)(1, 3, 5)
    }
    
    @Test def testTakeWhileWithIndex {
        val observable = Observable.from(1, 3, 5, 7, 9, 11, 12, 13, 15, 17)
        val took = observable.takeWhileWithIndex((i: Int, idx: Int) => isOdd(i) && idx < 8)
        assertSubscribeReceives(took)(1, 3, 5, 7, 9, 11)
    }
    
    @Test def testTakeLast {
        val observable = Observable.from(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val tookLast = observable.takeLast(3)
        assertSubscribeReceives(tookLast)(7, 8, 9)
    }
    
    @Test def testToList {
        val observable = Observable.from(1, 2, 3, 4)
        val toList = observable.toList
        assertSubscribeReceives(toList)(List(1, 2, 3, 4).asJava)
    }
    
    @Test def testToSortedList {
        val observable = Observable.from(1, 3, 4, 2)
        val toSortedList = observable.toSortedList
        assertSubscribeReceives(toSortedList)(List(1, 2, 3, 4).asJava)
    }
    
    @Test def testToArbitrarySortedList {
        val observable = Observable.from("a", "aaa", "aaaa", "aa")
        val sortByLength = (s1: String, s2: String) => s1.length.compareTo(s2.length)
        val toSortedList = observable.toSortedList(sortByLength)
        assertSubscribeReceives(toSortedList)(List("a", "aa", "aaa", "aaaa").asJava)
    }
    
    @Test def testToIterable {
        val observable = Observable.from(1, 2)
        val it = observable.toBlockingObservable.toIterable.iterator
        assertTrue(it.hasNext)
        assertEquals(1, it.next)
        assertTrue(it.hasNext)
        assertEquals(2, it.next)
        assertFalse(it.hasNext)
    }
    
    @Test def testStartWith {
        val observable = Observable.from(1, 2, 3, 4)
        val newStart = observable.startWith(-1, 0)
        assertSubscribeReceives(newStart)(-1, 0, 1, 2, 3, 4)
    }
    
    @Test def testOneLineForComprehension {
        val mappedObservable = for {
            i: Int <- Observable.from(1, 2, 3, 4)
        } yield i + 1
        assertSubscribeReceives(mappedObservable)(2, 3, 4, 5)
        assertFalse(mappedObservable.isInstanceOf[ScalaObservable[_]])
    }
    
    @Test def testSimpleMultiLineForComprehension {
        val flatMappedObservable = for {
            i: Int <- Observable.from(1, 2, 3, 4)
            j: Int <- Observable.from(1, 10, 100, 1000)
        } yield i + j
        assertSubscribeReceives(flatMappedObservable)(2, 12, 103, 1004)
        assertFalse(flatMappedObservable.isInstanceOf[ScalaObservable[_]])
    }
    
    @Test def testMultiLineForComprehension {
        val doubler = (i: Int) => Observable.from(i, i)
        val flatMappedObservable = for {
            i: Int <- Observable.from(1, 2, 3, 4)
            j: Int <- doubler(i)
        } yield j
        //can't use assertSubscribeReceives since each number comes in 2x
        flatMappedObservable.subscribe(observer.asInstanceOf[Observer[Int]])
        List(1, 2, 3, 4).foreach(i => verify(observer, times(2)).onNext(i))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
        assertFalse(flatMappedObservable.isInstanceOf[ScalaObservable[_]])
    }
    
    @Test def testFilterInForComprehension {
        val doubler = (i: Int) => Observable.from(i, i)
        val filteredObservable: Observable[Int] = for {
            i: Int <- Observable.from(1, 2, 3, 4)
            j: Int <- doubler(i) if isOdd(i)
        } yield j
        //can't use assertSubscribeReceives since each number comes in 2x
        filteredObservable.subscribe(observer.asInstanceOf[Observer[Int]])
        List(1, 3).foreach(i => verify(observer, times(2)).onNext(i))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
        assertFalse(filteredObservable.isInstanceOf[ScalaObservable[_]])
    }
    
    @Test def testForEachForComprehension {
        val doubler = (i: Int) => Observable.from(i, i)
        val intBuffer = ArrayBuffer.empty[Int]
        val forEachComprehension = for {
            i: Int <- Observable.from(1, 2, 3, 4)
            j: Int <- doubler(i) if isEven(i)
        } {
            intBuffer.append(j)
        }
        assertEquals(List(2, 2, 4, 4), intBuffer.toList)
    }
    
    private def assertSubscribeReceives[T](o: Observable[T])(values: T*) = {
        o.subscribe(observer.asInstanceOf[Observer[T]])
        values.toList.foreach(t => verify(observer, times(1)).onNext(t))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
    }
}
