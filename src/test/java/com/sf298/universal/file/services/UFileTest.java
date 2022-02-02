package com.sf298.universal.file.services;

import com.sf298.universal.file.model.responses.UFOperationResult;
import com.sf298.universal.file.services.dropbox.UFileDropbox;
import com.sf298.universal.file.services.dropbox.UFileDropboxBatch;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class UFileTest {

    private static final Random rand = new Random();
    private final UFile root;
    private final String file1 = "File1.txt";
    private final String file111;
    private final String folder1;
    private final String folder11;
    private final String folder111;
    private final UFile uFile1;
    private final UFile uFile11;
    private final UFile uFolder1;
    private final UFile uFolder11;
    private final UFile uFolder111;


    public UFileTest(UFile root) {
        this.root = root;

        this.file111 = "folder1"+root.getFileSep()+"folder11"+root.getFileSep()+"File1.txt";
        this.folder1 = "folder1";
        this.folder11 = "folder1"+root.getFileSep()+"folder11";
        this.folder111 = "folder1"+root.getFileSep()+"folder11"+root.getFileSep()+"folder111";

        this.uFile1 = root.goTo(file1);
        this.uFile11 = root.goTo(file111);
        this.uFolder1 = root.goTo(folder1);
        this.uFolder11 = root.goTo(folder11);
        this.uFolder111 = root.goTo(folder111);
    }

    @BeforeEach
    public void setup() {
        System.out.println("Start setup");
        System.out.println("A" + Arrays.toString(root.listFiles().getResult()));
        assert Arrays.stream(root.listFiles().getResult())
                .map(UFile::deleteRecursive)
                .allMatch(UFOperationResult::getResult);
        System.out.println("B" + Arrays.toString(root.listFiles().getResult()));
        assert Arrays.stream(root.listFiles().getResult())
                .map(UFile::deleteRecursive)
                .allMatch(UFOperationResult::getResult);
        System.out.println("C" + Arrays.toString(root.listFiles().getResult()));
        assert uFolder111.mkdirs().getResult();
        assert uFile1.createNewFile().getResult();
        assert uFile11.createNewFile().getResult();
        System.out.println("End setup");
    }

    @AfterEach
    public void close() {
        System.out.println("Start close");
        uFile1.close();
        uFile11.close();
        uFolder1.close();
        uFolder11.close();
        uFolder111.close();
        Arrays.stream(root.listFiles().getResult())
                .forEach(UFile::deleteRecursive);
        root.close();
        System.out.println("End close");
    }


    @Test
    public void testGetName() {
        UFile dir = root.goTo("a/b/c/");
        assertThat(dir.getName()).isEqualTo("c");

        UFile file = root.goTo("a/b/c.txt");
        assertThat(file.getName()).isEqualTo("c.txt");
    }

    @Test
    public void testGetParent() {
        assertThat(uFolder111.getParent()).isEqualTo(UFile.join(root.getPath(), folder11, root.getFileSep()));
        assertThat(uFile11.getParent()).isEqualTo(UFile.join(root.getPath(), folder11, root.getFileSep()));
        assertThat(uFile1.getParent()).isEqualTo(root.getPath());

        UFile f = root;
        boolean foundNull = false;
        for (int i = 0; i < 1000000; i++) {
            if(isNull(f)) {
                foundNull = true;
                break;
            }
            f = f.getParentUFile();
        }
        assertThat(foundNull).isTrue();
    }

    @Test
    public void testGetParentUFile() {
        assertThat(uFile1.getParentUFile()).isEqualTo(root);
        assertThat(uFolder11.getParentUFile()).isEqualTo(uFolder1);
    }

    @Test
    public void testGetPath() {
        String expected = UFile.join(root.getPath(), file1, root.getFileSep());
        assertThat(uFile1.getPath()).isEqualTo(expected);
    }


    @Test
    public void testExists() {
        assertThat(uFile1.exists().getResult()).isTrue();

        UFile rand = root.goTo("mkdirs/noExist.txt");
        assertThat(rand.exists().getResult()).isFalse();
    }

    @Test
    public void testIsDirectory() {
        assertThat(uFile1.isDirectory().getResult()).isFalse();
        assertThat(uFolder1.isDirectory().getResult()).isTrue();
        assertThat(uFolder11.isDirectory().getResult()).isTrue();
    }

    @Test
    public void testIsFile() {
        assertThat(uFile1.isFile().getResult()).isTrue();
        assertThat(uFolder1.isFile().getResult()).isFalse();
        assertThat(uFolder11.isFile().getResult()).isFalse();
    }

    @Test
    public void testLastModified() {
        long randomTime = (Math.abs(rand.nextInt())+1) * 1000L;
        assertThat(uFile1.setLastModified(new Date(randomTime)).getResult()).isTrue();
        assertThat(uFile1.lastModified().getResult().getTime()).isEqualTo(randomTime);
    }

    @Test
    @Order(0)
    public void testLength() {
        assertThat(uFile1.length().getResult()).isZero();
    }

    @Test
    public void testCreateNewFile() {
        UFile f = root.goTo("createNewFile.txt");
        assertThat(f.exists().getResult()).isFalse();
        assertThat(f.createNewFile().getResult()).isTrue();
        assertThat(f.exists().getResult()).isTrue();
        assertThat(f.createNewFile().getResult()).isFalse();
    }

    @Test
    public void testDelete() {
        assertThat(uFolder1.delete().getResult()).isFalse();
        assertThat(uFolder1.exists().getResult()).isTrue();

        assertThat(uFile11.delete().getResult()).isTrue();
        assertThat(uFile11.exists().getResult()).isFalse();

        assertThat(uFolder111.delete().getResult()).isTrue();
        assertThat(uFolder111.exists().getResult()).isFalse();

        assertThat(uFolder1.deleteRecursive().getResult()).isTrue();
        assertThat(uFolder1.exists().getResult()).isFalse();
    }


    @Test
    public void testList() {
        String[] filesFound = root.list().getResult();
        assertThat(filesFound).containsExactlyInAnyOrder(file1, folder1);
    }

    @Test
    public void testListFiltered() {
        String[] filesFound = root.list((dir, name) -> name.contains("i")).getResult();
        assertThat(filesFound).containsExactlyInAnyOrder(file1);
    }


    @Test
    public void testListFiles() {
        UFile[] filesFound = root.listFiles().getResult();
        assertThat(filesFound).containsExactlyInAnyOrder(uFile1, uFolder1);
    }

    @Test
    public void testListFilesFiltered() {
        UFile[] filesFound = root.listFiles((dir, name) -> name.contains("i")).getResult();
        assertThat(filesFound).containsExactlyInAnyOrder(uFile1);
    }

    @Test
    public void testListFilesUFileFilter() {
        UFile[] filesFound = root.listFiles((path) -> path.getName().contains("o")).getResult();
        assertThat(filesFound).containsExactlyInAnyOrder(uFolder1);
    }


    @Test
    @Order(1)
    public void testMkdir() {
        UFile folders = root.goTo("mkdir/mkdir2");
        assertThat(folders.mkdir().getResult()).isFalse();
        assertThat(folders.exists().getResult()).isFalse();
    }

    @Test
    @Order(0)
    public void testMkdirs() {
        UFile folders = root.goTo("mkdirs/mkdirs2");
        assertThat(folders.mkdirs().getResult()).isTrue();
        assertThat(folders.exists().getResult()).isTrue();
    }


    @Test
    public void testFileContents() throws IOException {
        String contents1 = "test File" + System.lineSeparator() + "Contents";
        String contents2 = "abc";

        PrintWriter writeStream = new PrintWriter(uFile1.write());
        writeStream.write(contents1);
        writeStream.close();
        uFile1.writeClose();

        Scanner s1 = new Scanner(uFile1.read()).useDelimiter("\\A");
        String result1 = s1.hasNext() ? s1.next() : "";
        s1.close();
        uFile1.readClose();
        assertThat(result1).isEqualTo(contents1);

        PrintWriter appendStream = new PrintWriter(uFile1.append());
        appendStream.write(contents2);
        appendStream.close();
        uFile1.appendClose();

        Scanner s2 = new Scanner(uFile1.read()).useDelimiter("\\A");
        String result2 = s2.hasNext() ? s2.next() : "";
        s2.close();
        uFile1.readClose();
        assertThat(result2).isEqualTo(contents1 + contents2);
    }


    @Test
    public void testCopyTo() throws IOException {
        String contents = "abc124";
        PrintWriter writeStream = new PrintWriter(uFile1.write());
        writeStream.write(contents);
        writeStream.close();
        uFile1.writeClose();

        UFile dest1 = root.goTo("notExist/file1.txt");
        UFile dest2 = root.goTo("folder1/file1.txt");

        assertThat(uFile1.copyTo(dest1).isSuccessful()).isFalse();
        assertThat(dest1.exists().getResult()).isFalse();

        uFile1.copyTo(dest2);
        assertThat(dest2.exists().getResult()).isTrue();
        assertThat(uFile1.exists().getResult()).isTrue();

        Scanner s1 = new Scanner(dest2.read()).useDelimiter("\\A");
        String result1 = s1.hasNext() ? s1.next() : "";
        s1.close();
        dest2.readClose();
        assertThat(result1).isEqualTo(contents);
    }

    @Test
    public void testMoveTo() throws IOException {
        String contents = "abc124";
        PrintWriter writeStream = new PrintWriter(uFile1.write());
        writeStream.write(contents);
        writeStream.close();
        uFile1.writeClose();

        UFile dest1 = root.goTo("notExist/file1.txt");
        UFile dest2 = root.goTo("folder1/file1.txt");

        assertThat(uFile1.moveTo(dest1).isSuccessful()).isFalse();
        assertThat(dest1.exists().getResult()).isFalse();

        uFile1.moveTo(dest2);
        assertThat(dest2.exists().getResult()).isTrue();
        assertThat(uFile1.exists().getResult()).isFalse();

        Scanner s1 = new Scanner(dest2.read()).useDelimiter("\\A");
        String result1 = s1.hasNext() ? s1.next() : "";
        s1.close();
        dest2.readClose();
        assertThat(result1).isEqualTo(contents);
    }


    @Test
    public void testGoTo() {

    }

    @Test
    public void testJoin() {
        assertThat(UFile.join("/a/b/c", "d", "/")).isEqualTo("/a/b/c/d");
        assertThat(UFile.join("/a/b/c/", "d", "/")).isEqualTo("/a/b/c/d");
        assertThat(UFile.join("/a/b/c", "/d", "/")).isEqualTo("/a/b/c/d");
        assertThat(UFile.join("/a/b/c/", "/d", "/")).isEqualTo("/a/b/c/d");

        assertThat(UFile.join("/a/b/c/", "../d", "/")).isEqualTo("/a/b/d");
        assertThat(UFile.join("/a/b/c/", "../../d", "/")).isEqualTo("/a/d");
    }

}
