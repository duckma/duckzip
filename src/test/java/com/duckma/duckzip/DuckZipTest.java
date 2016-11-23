package com.duckma.duckzip;

import io.reactivex.subscribers.TestSubscriber;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;

public class DuckZipTest {

    private DuckZip duckZip;
    private TestSubscriber<Float> testSubscriber;
    private File destFolder;

    @Mock
    private DuckZip.CurrentMillisCheck currentMillisCheck;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        duckZip = new DuckZip(currentMillisCheck);
        testSubscriber = TestSubscriber.create();
        destFolder = tempFolder.newFolder();
        destFolder.deleteOnExit();
    }

    @Test
    public void testUnzipNullParameters() throws Exception {
        duckZip.unzip(null, null)
                .subscribe(testSubscriber);
        testSubscriber.assertError(IllegalArgumentException.class);
    }

    @Test
    public void testUnzipEmptyParameters() throws Exception {
        duckZip.unzip("", "")
                .subscribe(testSubscriber);
        testSubscriber.assertError(FileNotFoundException.class);
    }

    @Test
    public void testUnzipArchive() throws Exception {
        Mockito.when(currentMillisCheck.currentTimeMillis())
                .thenReturn(System.currentTimeMillis());
        duckZip.unzip("src/test/res/kitten.zip", destFolder.getPath())
                .subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertComplete();
    }

    @Test
    public void testUnzipArchiveDefaultProgression() throws Exception {
        Mockito.when(currentMillisCheck.currentTimeMillis())
                .thenAnswer(new Answer<Long>() {
                    long startTime = System.currentTimeMillis();
                    @Override
                    public Long answer(InvocationOnMock invocation) throws Throwable {
                        startTime = startTime + 400;
                        return startTime;
                    }
                });
        duckZip.unzip("src/test/res/kitten.zip", destFolder.getPath())
                .subscribe(testSubscriber);
        Mockito.verify(currentMillisCheck, Mockito.times(testSubscriber.valueCount())).currentTimeMillis();
        testSubscriber.assertNoErrors();
        testSubscriber.assertComplete();
    }

    @Test
    public void testSyncUnzipArchiveDefaultProgression() throws Exception {
        DuckZip.UnzipProgressUpdateCallback callback = Mockito.spy(new DuckZip.UnzipProgressUpdateCallback() {
            float progress;
            @Override
            public void onUnzipProgressUpdate(Float progressUpdate) {
                Assert.assertTrue(progress < progressUpdate);
                progress = progressUpdate;
            }
        });
        Mockito.when(currentMillisCheck.currentTimeMillis())
                .thenAnswer(new Answer<Long>() {
                    long startTime = System.currentTimeMillis();
                    @Override
                    public Long answer(InvocationOnMock invocation) throws Throwable {
                        startTime = startTime + 400;
                        return startTime;
                    }
                });
        duckZip.unzip("src/test/res/kitten.zip", destFolder.getPath(), callback);
        Mockito.verify(currentMillisCheck,
                Mockito.times(Mockito.mockingDetails(callback).getInvocations().size())).currentTimeMillis();
    }

    @Test
    public void testZipNullParameters() throws Exception {
        duckZip.zip(null, null)
            .subscribe(testSubscriber);
        testSubscriber.assertError(IllegalArgumentException.class);
    }

    @Test
    public void testZipEmptyParameters() throws Exception {
        duckZip.zip("", "")
            .subscribe(testSubscriber);
        testSubscriber.assertError(FileNotFoundException.class);
    }

    @Test
    public void testZipFile() throws Exception {
        Mockito.when(currentMillisCheck.currentTimeMillis())
            .thenReturn(System.currentTimeMillis());
        duckZip.zip("src/test/res/kitten/kitty.jpg", destFolder.getPath())
            .subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertComplete();
    }

    @Test
    public void testZipDirectory() throws Exception {
        Mockito.when(currentMillisCheck.currentTimeMillis())
            .thenReturn(System.currentTimeMillis());
        duckZip.zip("src/test/res/kitten", destFolder.getPath())
            .subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertComplete();
    }


}
