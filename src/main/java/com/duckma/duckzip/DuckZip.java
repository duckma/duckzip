package com.duckma.duckzip;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DuckZip {

    private final CurrentMillisCheck currentMillisCheck;

    public enum UpdateInterval {
        UPDATE_INTERVAL_SLOW(1000),
        UPDATE_INTERVAL_DEFAULT(300),
        UPDATE_INTERVAL_FAST(100);

        private int millisInterval;

        UpdateInterval(int millisInterval) {
            this.millisInterval = millisInterval;
        }

        int getMillisInterval() {
            return millisInterval;
        }
    }

    public DuckZip() {
        this.currentMillisCheck = new CurrentMillisCheck() {
            @Override
            public long currentTimeMillis() {
                return System.currentTimeMillis();
            }
        };
    }

    DuckZip(CurrentMillisCheck currentMillisCheck) {
        this.currentMillisCheck = currentMillisCheck;
    }

    /**
     * Extracts the files in the archive at sourceFilePath to the destDirectoryPath.
     * It emits updates with the percentage of file extracted with a given interval.
     *
     * @param sourceFilePath        The path of the source archive file
     * @param updatesIntervalMillis Number of milliseconds to wait between consecutive
     *                              progress updates. Default 300ms, minimum 100ms.
     *                              Notice the lower the interval, the slower the unzipping process.
     * @param destDirectoryPath     The path for the destination directory
     * @return An {@link Flowable} emitting updates indicating the percentage of files extracted.
     */
    public Flowable<Float> unzip(final String sourceFilePath, final String destDirectoryPath,
                                 final UpdateInterval updatesIntervalMillis) {
        return Flowable.create(new FlowableOnSubscribe<Float>() {
            @Override
            public void subscribe(final FlowableEmitter<Float> emitter) {
                try {
                    unzip(
                        sourceFilePath,
                        destDirectoryPath,
                        new ZipProgressUpdateCallback() {
                            @Override
                            public void onZipProgressUpdate(Float progressUpdate) {
                                emitter.onNext(progressUpdate);
                            }
                        },
                        updatesIntervalMillis.getMillisInterval() >
                            UpdateInterval.UPDATE_INTERVAL_FAST.getMillisInterval() ?
                            updatesIntervalMillis :
                            UpdateInterval.UPDATE_INTERVAL_FAST);
                    emitter.onComplete();
                } catch (IOException ex) {
                    emitter.onError(ex);
                }
            }
        }, BackpressureStrategy.DROP);
    }

    public Flowable<Float> unzip(final String sourceFilePath, final String destDirectoryPath) {
        return unzip(sourceFilePath, destDirectoryPath, UpdateInterval.UPDATE_INTERVAL_DEFAULT);
    }

    /**
     * Unzips the archive from the sourceFilePath into the destDirectoryPath.
     * It emits progress updates through the {@link ZipProgressUpdateCallback} every
     * updateIntervalMillis.
     *
     * @param sourceFilePath              The path of the archive to unzip.
     * @param destDirectoryPath           The destination path for the unzip process.
     * @param zipProgressUpdateCallback A callback to receive progress updates.
     * @param updateIntervalMillis        The interval at which the updates will be fired.
     * @throws IOException If source or destination paths can't be accessed.
     */
    public void unzip(String sourceFilePath, String destDirectoryPath,
                      ZipProgressUpdateCallback zipProgressUpdateCallback,
                      UpdateInterval updateIntervalMillis) throws IOException {

        //Input sanitizing
        if (sourceFilePath == null) {
            throw new IllegalArgumentException("Please provide a sourceFilePath");
        }
        if (destDirectoryPath == null) {
            throw new IllegalArgumentException("Please provide a destinationFilePath");
        }

        File sourceFile = new File(sourceFilePath);
        File destinationDirectory = new File(destDirectoryPath);

        ZipInputStream zis = new ZipInputStream(
            new BufferedInputStream(new FileInputStream(sourceFile)));

        int fileCount = new ZipFile(sourceFile).size();

        try {
            ZipEntry ze;
            int count;
            int currentFileIndex = 0;
            long lastUpdateTime = System.currentTimeMillis();
            byte[] buffer = new byte[8192];

            while ((ze = zis.getNextEntry()) != null) {
                currentFileIndex++;
                // Only send progress updates if the given updateIntervalMillis has elapsed.
                long currentTimeMillis = currentMillisCheck.currentTimeMillis();
                if (currentTimeMillis - lastUpdateTime > updateIntervalMillis.getMillisInterval()) {
                    zipProgressUpdateCallback.onZipProgressUpdate((float) currentFileIndex / fileCount);
                    lastUpdateTime = currentTimeMillis;
                }

                File outputFile = new File(destinationDirectory, ze.getName());
                File dir = ze.isDirectory() ? outputFile : outputFile.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new FileNotFoundException("Failed to ensure directory: " +
                        dir.getAbsolutePath());
                }
                if (ze.isDirectory()) {
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(outputFile);

                try {
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    }

    public void unzip(String sourceFilePath, String destDirectoryPath,
                      ZipProgressUpdateCallback zipProgressUpdateCallback) throws IOException {
        unzip(sourceFilePath, destDirectoryPath, zipProgressUpdateCallback, UpdateInterval.UPDATE_INTERVAL_DEFAULT);
    }

    /**
     * Compress the file(s) at sourcePath to the destFilePath.
     * It emits updates with the percentage of file compressed with a given interval.
     *
     * @param sourcePath The path of the source. It can be either a file or a directory
     * @param destFilePath The path of the destination zip file
     * @param updateInterval Number of milliseconds to wait between consecutive
     *                       progress updates. Default 300ms, minimum 100ms.
     *                       Notice the lower the interval, the slower the zipping process.
     * @return An {@link Flowable} emitting updates indicating the percentage of files compressed.
     */
    public Flowable<Float> zip(final String sourcePath, final String destFilePath,
                               final UpdateInterval updateInterval) {
        return Flowable.create(new FlowableOnSubscribe<Float>() {
            @Override
            public void subscribe(final FlowableEmitter<Float> emitter) throws Exception {
                zip(sourcePath, destFilePath, new ZipProgressUpdateCallback() {
                    @Override
                    public void onZipProgressUpdate(Float progressUpdate) {
                        emitter.onNext(progressUpdate);
                    }
                }, updateInterval);
                emitter.onComplete();
            }
        }, BackpressureStrategy.DROP);
    }

    public Flowable<Float> zip(String sourcePath, final String destFilePath) {
        return zip(sourcePath, destFilePath, UpdateInterval.UPDATE_INTERVAL_DEFAULT);
    }

    /**
     * Zips the file/directory from the sourcePath into the destFilePath.
     * It emits progress updates through the {@link ZipProgressUpdateCallback} every
     * updateIntervalMillis.
     *
     * @param sourcePath                The path of the archive to unzip.
     * @param destFilePath              The destination path for the unzip process.
     * @param zipProgressUpdateCallback The interval at which the updates will be fired.
     * @param updateInterval            A callback to receive progress updates.
     * @throws IOException
     */
    public void zip(String sourcePath, String destFilePath,
                    ZipProgressUpdateCallback zipProgressUpdateCallback,
                    UpdateInterval updateInterval) throws IOException {
        if (sourcePath == null) {
            throw new IllegalArgumentException("Please provide a sourceFilePath");
        }
        if (destFilePath == null) {
            throw new IllegalArgumentException("Please provide a destinationFilePath");
        }

        File zipFile = new File(destFilePath);
        if (zipFile.exists()) {
            zipFile.delete();
        }

        FileOutputStream zipFos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(zipFos);
        File root = new File(sourcePath);
        List<File> files = getFiles(root);
        long lastUpdateTime = System.currentTimeMillis();

        for (int i = 0; i < files.size(); i++) {
            long currentTimeMillis = currentMillisCheck.currentTimeMillis();
            if (currentTimeMillis - lastUpdateTime > updateInterval.getMillisInterval()) {
                zipProgressUpdateCallback.onZipProgressUpdate((float) (i+1) / files.size());
                lastUpdateTime = currentTimeMillis;
            }

            File file = files.get(i);
            String path = file.getPath()
                .substring(root.isDirectory() ? root.getPath().length() : root.getParent().length());

            ZipEntry zipEntry = new ZipEntry(path);
            zos.putNextEntry(zipEntry);

            FileInputStream fis = new FileInputStream(new File(file.getAbsolutePath()));
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            fis.close();
            zos.closeEntry();
        }

        zos.finish();
        zos.close();
    }

    private List<File> getFiles(File file) {
        List<File> result = new ArrayList<>();
        if (!file.isDirectory()) {
            result.add(file);
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    result.addAll(getFiles(child));
                }
            }
        }
        return result;
    }

    public void zip(String sourceFilePath, String destDirectoryPath,
                    ZipProgressUpdateCallback zipProgressUpdateCallback) throws IOException {
        zip(sourceFilePath, destDirectoryPath, zipProgressUpdateCallback, UpdateInterval.UPDATE_INTERVAL_DEFAULT);
    }

    public interface ZipProgressUpdateCallback {
        void onZipProgressUpdate(Float progressUpdate);
    }

    interface CurrentMillisCheck {
        long currentTimeMillis();
    }

}
