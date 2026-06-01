package joselusc.libraries.file2file.converters;

public class Benchmark {
    public static void main(String[] args) throws Exception {
        String script = "setenv VAR value\nset var = 1\nif ($var == 1) then\necho Yes\nendif\n";
        StringBuilder largeScript = new StringBuilder();
        for (int i=0; i<50000; i++) {
            largeScript.append(script);
        }
        String content = largeScript.toString();

        Csh2ShConverter converter = Csh2ShConverter.getInstance();

        // Warmup
        for(int i=0; i<2; i++) {
             converter.convertContent(content);
        }

        long start = System.currentTimeMillis();
        for(int i=0; i<5; i++) {
            converter.convertContent(content);
        }
        long end = System.currentTimeMillis();

        System.out.println("BASELINE: Total Time (5 runs): " + (end - start) + " ms");
    }
}
