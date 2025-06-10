/* ==================================================================
 * ResourceStorageServiceDirectoryWatcherTests.java - 19/10/2019 5:08:16 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.upload.resource.test;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.util.stream.Collectors.toSet;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.upload.resource.ResourceStorageServiceDirectoryWatcher;
import net.solarnetwork.service.ResourceStorageService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.CallingThreadExecutorService;

/**
 * Test cases for the {@link ResourceStorageServiceDirectoryWatcher} class.
 * 
 * @author matt
 * @version 2.0
 */
public class ResourceStorageServiceDirectoryWatcherTests {

	private final String TEST_SOURCE_ID = "test.source.id";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Path tmpDir;
	private ResourceStorageService storageService;
	private DatumQueue datumQueue;
	private ResourceStorageServiceDirectoryWatcher watcher;

	@Before
	public void setup() throws IOException {
		tmpDir = Files.createTempDirectory("test-storage-service-dir-watcher-");
		storageService = EasyMock.createMock(ResourceStorageService.class);
		datumQueue = EasyMock.createMock(DatumQueue.class);
		watcher = new ResourceStorageServiceDirectoryWatcher(new StaticOptionalService<>(storageService),
				new CallingThreadExecutorService());
		watcher.setPath(tmpDir.toAbsolutePath().toString());
		watcher.setResourceStorageDatumSourceId(TEST_SOURCE_ID);
		watcher.setDatumQueue(new StaticOptionalService<>(datumQueue));
	}

	@After
	public void teardown() throws IOException {
		EasyMock.verify(storageService, datumQueue);
		watcher.shutdown();
		if ( tmpDir != null ) {
			log.info("Deleting dir {}", tmpDir);
			Files.walk(tmpDir).forEach(p -> {
				try {
					Files.delete(p);
				} catch ( IOException e ) {
					// ignore
				}
			});
		}
	}

	private void replayAll() {
		EasyMock.replay(storageService, datumQueue);
	}

	private Path createFileInWatchDir() throws IOException {
		Path tmpFile = Files.createTempFile("test-storage-service-", ".txt");
		FileCopyUtils.copy("Hello, world.".getBytes(), tmpFile.toFile());
		Path destFile = tmpDir.resolve(tmpFile.getFileName());
		log.info("Creating watch dir file {}", destFile);
		return Files.move(tmpFile, destFile, ATOMIC_MOVE);
	}

	@Test
	public void createFile_noDatum() throws Exception {
		// GIVEN
		Capture<String> pathCaptor = Capture.newInstance();
		Capture<Resource> resourceCaptor = Capture.newInstance();
		CompletableFuture<Boolean> saveResult = new CompletableFuture<>();
		expect(storageService.saveResource(capture(pathCaptor), capture(resourceCaptor), eq(true),
				anyObject())).andAnswer(new IAnswer<CompletableFuture<Boolean>>() {

					@Override
					public CompletableFuture<Boolean> answer() throws Throwable {
						saveResult.complete(true);
						return saveResult;
					}
				});

		watcher.setResourceStorageDatumSourceId(null);

		// WHEN
		replayAll();
		watcher.startup();

		Thread.sleep(500);

		Path newFile = createFileInWatchDir();

		// THEN
		saveResult.get(1, TimeUnit.MINUTES);

		assertThat("Saved path is filename of created file", pathCaptor.getValue(),
				equalTo(newFile.getFileName().toString()));
		assertThat("Saved resource is created file", resourceCaptor.getValue().getFile(),
				equalTo(newFile.toFile()));
	}

	@Test
	public void createFile() throws Exception {
		// GIVEN
		Capture<String> pathCaptor = Capture.newInstance(CaptureType.ALL);
		Capture<Resource> resourceCaptor = Capture.newInstance();
		CompletableFuture<Boolean> saveResult = new CompletableFuture<>();
		expect(storageService.saveResource(capture(pathCaptor), capture(resourceCaptor), eq(true),
				anyObject())).andAnswer(new IAnswer<CompletableFuture<Boolean>>() {

					@Override
					public CompletableFuture<Boolean> answer() throws Throwable {
						saveResult.complete(true);
						return saveResult;
					}
				});

		URL storageUrl = new URI("http://example.com/file.txt").toURL();
		expect(storageService.resourceStorageUrl(capture(pathCaptor))).andReturn(storageUrl);

		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		watcher.startup();

		Thread.sleep(500);

		Path newFile = createFileInWatchDir();

		// THEN
		saveResult.get(1, TimeUnit.MINUTES);

		// give time for thread to finish datum generation tasks
		Thread.sleep(300);

		assertThat("Saved path is filename of created file",
				pathCaptor.getValues().stream().collect(toSet()),
				contains(newFile.getFileName().toString()));
		assertThat("Saved resource is created file", resourceCaptor.getValue().getFile(),
				equalTo(newFile.toFile()));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Generated datum source ID", d.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Generated datum date equal to file modification time", d.getTimestamp(),
				equalTo(Files.getLastModifiedTime(newFile).toInstant()));
		assertThat("Generated datum property URL is storage URL",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "url"),
				equalTo(storageUrl.toString()));
		assertThat("Generated datum property path is storage path",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "path"),
				equalTo(newFile.getFileName().toString()));
		assertThat("Generated datum property size is file size",
				d.asSampleOperations().getSampleLong(DatumSamplesType.Instantaneous, "size"),
				equalTo(Files.size(newFile)));
	}
}
