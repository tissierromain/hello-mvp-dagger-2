package com.example.bradcampbell.data;

import static com.example.bradcampbell.data.HelloDiskCache.KEY_DATA;
import static com.example.bradcampbell.data.HelloDiskCache.KEY_TIMESTAMP;
import static com.example.bradcampbell.data.HelloDiskCache.NO_VALUE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import com.example.bradcampbell.domain.HelloEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import rx.observers.TestSubscriber;

@RunWith(RobolectricTestRunner.class)
public class HelloDiskCacheTests {
    @Mock SharedPreferences prefs;

    private HelloDiskCache cache;

    @Before public void setup() {
        MockitoAnnotations.initMocks(this);
        cache = new HelloDiskCache(prefs);
    }

    @Test public void testEmptyCache() {
        when(prefs.getInt(KEY_DATA, NO_VALUE)).thenReturn(NO_VALUE);

        TestSubscriber<HelloEntity> testSubscriber = new TestSubscriber<>();
        cache.getEntity().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertReceivedOnNext(singletonList(null));
    }

    @Test public void testNonEmptyCache() {
        when(prefs.getInt(KEY_DATA, NO_VALUE)).thenReturn(1);
        when(prefs.getLong(KEY_TIMESTAMP, 0L)).thenReturn(2L);

        HelloEntity expectedResult = HelloEntity.create(1, 2L);

        TestSubscriber<HelloEntity> testSubscriber = new TestSubscriber<>();
        cache.getEntity().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertReceivedOnNext(singletonList(expectedResult));
    }

    @SuppressLint("CommitPrefEdits")
    @Test public void testSavingToDisk() {
        HelloEntity expectedResult = HelloEntity.create(1, 2L);

        SharedPreferences.Editor editorMock = mock(SharedPreferences.Editor.class);
        when(prefs.edit()).thenReturn(editorMock);

        class IncrementCountAnswer implements Answer {
            private int count = 0;
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                count++;
                return TRUE;
            }
        }

        IncrementCountAnswer incrementCount = new IncrementCountAnswer();
        doAnswer(incrementCount).when(editorMock).commit();
        doAnswer(incrementCount).when(editorMock).apply();

        when(editorMock.putInt(any(String.class), any(Integer.class))).thenReturn(editorMock);
        when(editorMock.putLong(any(String.class), any(Long.class))).thenReturn(editorMock);

        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        cache.saveEntity(expectedResult).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertReceivedOnNext(singletonList(TRUE));

        verify(editorMock).putInt(KEY_DATA, 1);
        verify(editorMock).putLong(KEY_TIMESTAMP, 2L);

        assertEquals(1, incrementCount.count);
    }

    @SuppressLint("CommitPrefEdits")
    @Test public void testClearingCache() {
        SharedPreferences.Editor editorMock = mock(SharedPreferences.Editor.class);
        when(prefs.edit()).thenReturn(editorMock);
        when(editorMock.remove(any(String.class))).thenReturn(editorMock);

        class IncrementCountAnswer implements Answer {
            private int count = 0;
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                count++;
                return TRUE;
            }
        }
        IncrementCountAnswer incrementCount = new IncrementCountAnswer();
        doAnswer(incrementCount).when(editorMock).commit();
        doAnswer(incrementCount).when(editorMock).apply();

        cache.clear();

        verify(editorMock).remove(KEY_DATA);
        verify(editorMock).remove(KEY_TIMESTAMP);

        assertEquals(1, incrementCount.count);
    }
}
