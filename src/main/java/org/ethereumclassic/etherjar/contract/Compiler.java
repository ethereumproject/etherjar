package org.ethereumclassic.etherjar.contract;

import org.ethereumclassic.etherjar.model.HexData;

import java.io.*;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Igor Artamonov
 */
public class Compiler {

    private File solc;

    public Compiler(String solc) {
        this.solc = new File(solc);
    }

    public Result compile(File source, boolean optimize) throws IOException, InterruptedException {
        return compile(new FileInputStream(source), optimize);
    }

    public Result compile(String source, boolean optimize) throws IOException, InterruptedException {
        return compile(new ByteArrayInputStream(source.getBytes("UTF-8")), optimize);
    }

    public Result compile(InputStream source, boolean optimize) throws IOException, InterruptedException {
        Path tmp = Files.createTempDirectory("etherjar-compile");
        Path contractSource = Files.createTempFile(tmp, "contract", ".sol");
        Files.copy(source, contractSource, StandardCopyOption.REPLACE_EXISTING);

        Process process = executeSolc(tmp, contractSource, optimize);

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        List<String> stdoutLines = new ArrayList<>();
        List<String> stderrLines = new ArrayList<>();
        while ((line = stdout.readLine()) != null) {
            stdoutLines.add(line);
        }
        while ((line = stderr.readLine()) != null) {
            stderrLines.add(line);
        }

        int status = process.waitFor();

        Result result;
        if (status != 0) {
            result = new Result(false);
        } else {
            result = processCompiledResult(tmp);
        }
        result.setStderr(stderrLines);
        result.setStdout(stdoutLines);
        clean(tmp);
        return result;
    }

    public Process executeSolc(Path dir, Path contractSource, boolean optimize) throws IOException {
        List<String> args = new ArrayList<>();
        args.add(solc.getAbsolutePath());
        if (optimize) {
            args.add("--optimize");
        }
        args.add("--abi");
        args.add("--bin");
        args.add("--output-dir");
        args.add("./");
        args.add("./" + contractSource.getFileName().toString());

        ProcessBuilder processBuilder = new ProcessBuilder(args)
            .directory(dir.toFile());
        return processBuilder.start();
    }

    public Result processCompiledResult(Path dir) throws IOException {
        List<String> errors = new ArrayList<>();
        List<CompiledContract> contracts = Files.list(dir).filter((f) ->
            f.getFileName().toString().endsWith(".bin")
        ).map((path -> {
            String name = path.getFileName().toString();
            return name.substring(0, name.length() - ".bin".length());
        })).map((name) -> {
            Path bin = dir.resolve(name + ".bin");
            HexData binData = null;
            try {
                if (bin.toFile().length() > 0) {
                    binData = HexData.from(
                        "0x" + String.join("", Files.readAllLines(bin))
                    );
                }
            } catch (IOException e) {
                errors.add(e.getMessage());
            }

            Path abi = dir.resolve(name + ".abi");
            String json = null;
            try {
                json = String.join("", Files.readAllLines(abi));
            } catch (IOException e) {
                errors.add(e.getMessage());
            }

            return new CompiledContract(name, binData, json);
        }).collect(Collectors.toList());

        Result result = new Result(true);
        result.add(contracts);
        result.setErrors(errors);
        return result;
    }

    private void clean(Path tmp) throws IOException {
        Files.walk(tmp, FileVisitOption.FOLLOW_LINKS)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    public static class Result {
        private boolean success;
        private List<String> stdout;
        private List<String> stderr;
        private List<String> errors;
        private List<CompiledContract> contracts;

        public Result(boolean success) {
            this.success = success;
            contracts = new ArrayList<>();
        }

        public List<String> getNames() {
            return contracts.stream()
                .map(CompiledContract::getName)
                .collect(Collectors.toList());
        }

        public CompiledContract getContract(String name) {
            for(CompiledContract c: contracts) {
                if (c.getName().equals(name)) {
                    return c;
                }
            }
            return null;
        }

        public List<CompiledContract> getContracts() {
            return contracts;
        }

        public Result add(CompiledContract contract) {
            contracts.add(contract);
            return this;
        }
        public Result add(List<CompiledContract> contracts) {
            this.contracts.addAll(contracts);
            return this;
        }

        public int getCount() {
            return contracts.size();
        }

        public boolean isSuccess() {
            return success;
        }

        public List<String> getStdout() {
            return stdout;
        }

        public void setStdout(List<String> stdout) {
            this.stdout = stdout;
        }

        public List<String> getStderr() {
            return stderr;
        }

        public void setStderr(List<String> stderr) {
            this.stderr = stderr;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }

    public static class CompiledContract {
        private final String name;
        private HexData compiled;
        private String abi;

        public CompiledContract(String name) {
            this.name = name;
        }

        public CompiledContract(String name, HexData compiled, String abi) {
            this(name);
            this.compiled = compiled;
            this.abi = abi;
        }

        public void setCompiled(HexData compiled) {
            this.compiled = compiled;
        }

        public void setAbi(String abi) {
            this.abi = abi;
        }

        public HexData getCompiled() {
            return compiled;
        }

        public String getAbi() {
            return abi;
        }

        public String getName() {
            return name;
        }
    }

}