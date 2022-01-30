package com.sf298.universal.file.services;

import com.sf298.universal.file.model.responses.UFExistsResult;
import com.sf298.universal.file.model.responses.UFOperationBatchResult;
import com.sf298.universal.file.model.responses.UFOperationResult;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UFileBatchManagerTest {

    private static final Random rand = new Random();
    private final UFile[] roots;
    private final UFile[] uFile1;
    private final UFile[] uFile111;
    private final UFile[] uFolder1;
    private final UFile[] uFolder11;
    private final UFile[] uFolder111;

    public UFileBatchManagerTest(UFile[] roots) {
        this.roots = roots;
        this.uFile1 = new UFile[roots.length];
        this.uFile111 = new UFile[roots.length];
        this.uFolder1 = new UFile[roots.length];
        this.uFolder11 = new UFile[roots.length];
        this.uFolder111 = new UFile[roots.length];

        for (int i = 0; i < roots.length; i++) {
            this.uFile1[i] = this.roots[i].goTo("File1.txt");
            this.uFile111[i] = this.roots[i].goTo("folder1"+ this.roots[i].getFileSep()+"folder11"+ this.roots[i].getFileSep()+"File1.txt");
            this.uFolder1[i] = this.roots[i].goTo("folder1");
            this.uFolder11[i] = this.roots[i].goTo("folder1"+ this.roots[i].getFileSep()+"folder11");
            this.uFolder111[i] = this.roots[i].goTo("folder1"+ this.roots[i].getFileSep()+"folder11"+ this.roots[i].getFileSep()+"folder111");
        }
    }

    @BeforeEach
    public void setup() {
        List<UFile> filesToMk = UFileBatchManager.existsBatch(asList(roots)).stream()
                .filter(uFileExistsResult -> !uFileExistsResult.isSuccessful())
                .map(UFOperationResult::getActionedFile)
                .collect(Collectors.toList());
        UFileBatchManager.mkdirsBatch(filesToMk);
        Arrays.stream(roots.listFiles())
                .forEach(uf -> uf.delete(true));
        uFile1.createNewFile();
        uFolder1.mkdirs();
        uFolder11.mkdirs();
    }

    @AfterEach
    public void close() {
        roots.close();
        uFile1.close();
        uFolder1.close();
        uFolder11.close();
        Arrays.stream(roots.listFiles())
                .forEach(uf -> uf.delete(true));
    }


    @Test
    @Order(1)
    public void testExistsBatch() {
        List<UFile> inputs = List.of(uFile1, uFile111, roots.goTo("mkdirs/noExist.txt"));
        List<Boolean> expected = List.of(true, true, false);

        UFOperationBatchResult<UFExistsResult> actual = UFileBatchManager.existsBatch(inputs);

        for (int i = 0; i < expected.size(); i++) {
            assertThat(actual.get(i).isSuccessful()).isEqualTo(expected.get(i));
        }

        UFile rand = roots.goTo("mkdirs/noExist.txt");
        assertThat(rand.exists().isSuccessful()).isFalse();
    }

}