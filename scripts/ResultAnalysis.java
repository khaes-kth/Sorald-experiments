package sorald;

import models.RuleRepairStat;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    static String[] CONSIDERED_RULES = new String[] {"1217", "1860", "2095", "2111", "2116", "2142", "2184", "2225", "2272", "4973"};

    public static void main(String[] args) throws IOException {
        generateLatestTable();
    }

    private static void countRQ2SelectedRepos() throws IOException {
        System.out.println(FileUtils.readLines(new File("file/sorald/selected-commits.csv"), "UTF-8")
                .stream().map(x -> {return x.split(",")[0];}).collect(Collectors.toSet()).size());
    }

    private static void analyzeRQ2Results() throws IOException {
        List<String> lines = FileUtils.readLines(new File("file/sorald/soraldbot-patches.txt"), "UTF-8");
        Set<String> repos = new HashSet<>(), commits = new HashSet<>();
        Map<String, Set<String>> ruleToRepos = new HashMap<>(), ruleToCommits = new HashMap<>();
        Map<String, Integer> ruleToViolationCnt = new HashMap<>();
        int totalFixCnt = 0;
        for(int i = 1; i < lines.size(); i++){
            String line = lines.get(i).replace("https://github.com/khesoem/", "");
            int cnt = Integer.parseInt(line.split(",")[1]);
            line = line.replace("," + cnt, "");
            String rule = line.split("_")[1];
            line = line.replace("_" + rule, "");
            String repo = line.split("/")[0];
            String[] parts = line.split("/");
            String commit = parts[parts.length - 1].replace("repairnator-patch-", "");
            repos.add(repo);
            commits.add(commit);
            if(!ruleToRepos.containsKey(rule)){
                ruleToRepos.put(rule, new HashSet<>());
                ruleToCommits.put(rule, new HashSet<>());
                ruleToViolationCnt.put(rule, 0);
            }
            ruleToRepos.get(rule).add(repo);
            ruleToCommits.get(rule).add(commit);
            ruleToViolationCnt.put(rule, ruleToViolationCnt.get(rule) + cnt);
            totalFixCnt += cnt;
        }

        System.out.println("REPOS");
        ruleToRepos.entrySet().forEach(e -> System.out.println(e.getKey() + " " + e.getValue().size()));
        System.out.println("PATCHES");
        ruleToCommits.entrySet().forEach(e -> System.out.println(e.getKey() + " " + e.getValue().size()));
        System.out.println("VIOLATIONS");
        ruleToViolationCnt.entrySet().forEach(e -> System.out.println(e.getKey() + " " + e.getValue()));

        System.out.println("total repos: " + repos.size());
        System.out.println("total commits: " + commits.size());
        System.out.println("total patches: " + ruleToCommits.values().stream().mapToInt(Set::size).sum());
        System.out.println("total fixes: " + totalFixCnt);
    }

    private static Set<String> extractAnalysisResultsAndGetFixableAndPartiallyFixable() throws IOException {
        Set<String> res = new HashSet<>();
        List<String> lines = FileUtils.readLines(new File("file/sorald/analysis.csv"), "UTF-8");

        int[][] cnt = new int[][]{new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}};
        for (int i = 1; i <= 153; i++) {
            String line = lines.get(i);
            String[] parts = line.split(",");
            int withInstanceInd = Integer.parseInt(parts[parts.length - 1]) > 0 ? 1 : 0;
            String rule = "S" + parts[parts.length - 3].split("-")[1];
            int typeInd = -1;
            if (parts[6].length() == 0) {
                typeInd = getInd(parts[1]);
                cnt[typeInd][withInstanceInd]++;
                if(typeInd == 0 || typeInd == 2)
                    res.add(rule);
            } else if (getInd(parts[6]) >= 0) {
                typeInd = getInd(parts[6]);
                cnt[typeInd][withInstanceInd]++;
                if(typeInd == 0 || typeInd == 2)
                    res.add(rule);
            } else {
                typeInd = getInd(parts[11]);
                cnt[typeInd][withInstanceInd]++;
                if(typeInd == 0 || typeInd == 2)
                    res.add(rule);
            }
        }

        for (int i = 0; i < cnt.length; i++) {
            System.out.println(cnt[i][0] + "," + cnt[i][1]);
        }

        return res;
    }

    private static int getInd(String label) {
        if (label.toLowerCase().equals("yes")) {
            return 0;
        } else if (label.toLowerCase().equals("no")) {
            return 1;
        } else if (label.toLowerCase().equals("partially")) {
            return 2;
        }
        return -1;
    }

    private static void countDetectedViolationsOfFixableAndPartiallyFixable() throws IOException {
        File outputDir = new File("/home/khaes/phd/projects/sorald/tmp/new-rq1");
        int totalFixableAndPartiallyFixableViolationCnt = 0;
        Set<String> fixableAndPartiallyFixable = extractAnalysisResultsAndGetFixableAndPartiallyFixable();
        Map<String, Integer> ruleToMinedCnt = new HashMap<>();

        for(File f : outputDir.listFiles()){
            if(f.getName().endsWith("_sorald_mine_stats.json")){
                JSONObject jo = new JSONObject(FileUtils.readFileToString(f, "UTF-8"));
                JSONArray ja = jo.getJSONArray("minedRules");
                for(int i = 0; i < ja.length(); i++){
                    jo = (JSONObject) ja.get(i);
                    String rule = jo.getString("ruleKey");
                    if(!rule.startsWith("S"))
                        rule = "S" + rule;
                    if(!fixableAndPartiallyFixable.contains(rule))
                        continue;
                    JSONArray warnings = jo.getJSONArray("warningLocations");
                    if(!ruleToMinedCnt.containsKey(rule))
                        ruleToMinedCnt.put(rule, 0);
                    ruleToMinedCnt.put(rule, ruleToMinedCnt.get(rule) + warnings.length());
                    totalFixableAndPartiallyFixableViolationCnt += warnings.length();
                }
            }
        }

        System.out.println(totalFixableAndPartiallyFixableViolationCnt);

        ArrayList<Map.Entry<String, Integer>> entryLst = new ArrayList<Map.Entry<String, Integer>>(ruleToMinedCnt.entrySet());
        entryLst.sort(Map.Entry.comparingByValue());
        entryLst.forEach(System.out::println);
    }

    private static void generateLatestTable() throws IOException {
        String repairStatsFileSuffix = "_sorald_repair_stats.json";
//        File outputDir = new File("/home/khaes/phd/projects/sorald/tmp/old-rq1/RQ1/output-2021-12-12-18-34-02");
        File outputDir = new File("/home/khaes/phd/projects/sorald/tmp/new-rq1");
        Map<String, RuleRepairStat> ruleToSuccessStat = new HashMap<>(), ruleToAllFailStat = new HashMap<>(),
                latestRes = new HashMap<>();
        Map<String, Integer> ruleToMinedCnt = new HashMap<>();
        Map<String, Set<String>> ruleToMined = new HashMap<>();

        for(File f : outputDir.listFiles()){
            if(f.getName().endsWith("_sorald_mine_stats.json")){
                JSONObject jo = new JSONObject(FileUtils.readFileToString(f, "UTF-8"));
                JSONArray ja = jo.getJSONArray("minedRules");
                for(int i = 0; i < ja.length(); i++){
                    jo = (JSONObject) ja.get(i);
                    String rule = jo.getString("ruleKey");
                    if(!rule.startsWith("S"))
                        rule = "S" + rule;
                    if(!Arrays.asList(CONSIDERED_RULES).contains(rule.substring(1)))
                        continue;
                    JSONArray warnings = jo.getJSONArray("warningLocations");
                    if(!ruleToMinedCnt.containsKey(rule)) {
                        ruleToMinedCnt.put(rule, 0);
                        ruleToMined.put(rule, new HashSet<>());
                    }
                    for(int j = 0; j < warnings.length(); j++){
                        ruleToMined.get(rule).add(((JSONObject)warnings.get(j)).getString("violationSpecifier"));
                    }
                    int cnt = warnings.length();
                    ruleToMinedCnt.put(rule, ruleToMinedCnt.get(rule) + cnt);
                }
            }
        }

        for(File f : outputDir.listFiles()){
            if(f.getName().contains(repairStatsFileSuffix)){
                String[] filenameParts = f.getName().replace(repairStatsFileSuffix, "").split("_");
                String rule = "S" + filenameParts[filenameParts.length - 1];
                if(!ruleToSuccessStat.containsKey(rule)) {
                    ruleToSuccessStat.put(rule, new RuleRepairStat());
                    ruleToAllFailStat.put(rule, new RuleRepairStat());
                    latestRes.put(rule, new RuleRepairStat());
                }

                JSONObject jo = new JSONObject(FileUtils.readFileToString(f, "UTF-8"));
                JSONArray ja = (JSONArray) jo.get("repairs");
                if(ja.length() > 0){
                    jo = (JSONObject) ja.get(0);
                    int nbBefore = jo.getInt("nbViolationsBefore"),
                            nbPerformed = jo.getInt("nbPerformedRepairs"),
                            nbAfter = jo.getInt("nbViolationsAfter");

                    JSONArray performedFixes = (JSONArray) jo.get("performedRepairsLocations");
                    for(int j = 0; j < performedFixes.length(); j++){
                        if(!ruleToMined.get(rule)
                                .contains(((JSONObject)performedFixes.get(j)).getString("violationSpecifier"))){
                            System.out.println(((JSONObject)performedFixes.get(j)).getString("violationSpecifier"));
                        }
                    }

                    File mvnFile = new File(f.getPath().replace("sorald_repair_stats.json", "mvn.log"));

                    String mvnOutput = FileUtils.readFileToString(mvnFile, "UTF-8");
                    if(mvnOutput.contains("COMPILATION ERROR")){
                        ruleToAllFailStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);
                    } else if ((mvnOutput.contains("Checkstyle violation") ||
                            mvnOutput.contains("Failed during checkstyle execution")) &&
                            mvnFile.getPath().contains("2095") && !mvnFile.getPath().contains("YCSB")) {
                        ruleToAllFailStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);
                    } else if(!mvnOutput.contains("BUILD SUCCESS")){
                        ruleToSuccessStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);
                        if(mvnFile.getName().contains("2095"))
                            System.out.println(mvnFile.getName());
                    } else{
                        ruleToSuccessStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);if(mvnFile.getName().contains("2095"))
                            System.out.println(mvnFile.getName());
                    }
                }
            }
        }

        latestRes.entrySet().forEach(e -> {
            String rule = e.getKey();
            RuleRepairStat stat = e.getValue();
            int before = ruleToSuccessStat.get(rule).getNbBefore() + ruleToAllFailStat.get(rule).getNbBefore(),
                    fixed = ruleToSuccessStat.get(rule).getNbFixed(),
                    performed = ruleToSuccessStat.get(rule).getNbPerformed() + ruleToAllFailStat.get(rule).getNbPerformed();

            stat.addStat(before, performed, before - fixed);
        });

        System.out.println("LATEST TABLE");
        generateTable(latestRes, ruleToMinedCnt);
    }

    private static void worstAndBestCaseTableGenerator() throws IOException {
        String repairStatsFileSuffix = "_sorald_repair_stats.json";
        File outputDir = new File("/home/khaes/phd/projects/sorald/tmp/new-rq1/RQ1/output-2021-12-10-15-02-12");
        Map<String, Integer> ruleToAll = new HashMap<>();
        Set<String> consideredRepos = new HashSet<>();
        Map<String, RuleRepairStat> ruleToSuccessStat = new HashMap<>(), ruleToIrreparableFailStat = new HashMap<>(),
                ruleToAllFailStat = new HashMap<>(), bestCase = new HashMap<>(), worstCase = new HashMap<>();

        for(File f : outputDir.listFiles()){
            if(f.getName().contains(repairStatsFileSuffix)){
                JSONObject jo = new JSONObject(FileUtils.readFileToString(f, "UTF-8"));
                JSONArray ja = (JSONArray) jo.get("repairs");
                if(ja.length() > 0){
                    jo = (JSONObject) ja.get(0);
                    int nbBefore = jo.getInt("nbViolationsBefore"),
                            nbPerformed = jo.getInt("nbPerformedRepairs"),
                            nbAfter = jo.getInt("nbViolationsAfter");

                    String rule = jo.getString("ruleKey");
                    if(!ruleToSuccessStat.containsKey(rule)) {
                        ruleToSuccessStat.put(rule, new RuleRepairStat());
                        ruleToAllFailStat.put(rule, new RuleRepairStat());
                        ruleToIrreparableFailStat.put(rule, new RuleRepairStat());
                        bestCase.put(rule, new RuleRepairStat());
                        worstCase.put(rule, new RuleRepairStat());
                    }

                    File mvnFile = new File(f.getPath().replace("sorald_stats.json", "mvn.log"));


                    if(rule.contains("1217")){
                        String repo = mvnFile.getPath().split("/")[8].split("_")[0];
                        ruleToAll.put(repo, nbBefore);
                        consideredRepos.add(repo);
                        System.out.println(mvnFile.getPath());
                    }


                    String mvnOutput = FileUtils.readFileToString(mvnFile, "UTF-8");
                    if(mvnOutput.contains("Compilation failure")){
                        ruleToAllFailStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);
                        if(mvnFile.getPath().contains("2095"))
                            ruleToIrreparableFailStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);
                    } else if ((mvnOutput.contains("Checkstyle violation") ||
                            mvnOutput.contains("Failed during checkstyle execution")) &&
                            mvnFile.getPath().contains("2095") && !mvnFile.getPath().contains("YCSB")) {
                        ruleToAllFailStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);
                        ruleToIrreparableFailStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);
                    } else{
                        ruleToSuccessStat.get(rule).addStat(nbBefore, nbPerformed, nbAfter);
                    }
//                    if(mvnOutput.contains("BUILD SUCCESS") || mvnOutput.contains("Failed to collect dependencies")
//                            || mvnOutput.contains("Non-resolvable import POM") ||
//                            ((mvnOutput.contains("Checkstyle violation") ||
//                                    mvnOutput.contains("Failed during checkstyle execution")) && f.getPath().contains("YCSB"))
//                            || (mvnOutput.contains("There are test failures") && !mvnFile.getPath().contains("openhtmltopdf_2095")
//                            || (mvnOutput.contains("Compilation failure") && !mvnFile.getPath().contains("2095")))
//                            ) {
//                        success++;
//                        ruleToStat.get(rule).addSuccessStat(nbBefore, nbPerformed, nbAfter);
//                    } else {
//                        fail++;
//                        ruleToStat.get(rule).addFailStat(nbBefore, nbPerformed, nbAfter);
//
//                        compilation += mvnOutput.contains("Compilation failure") || mvnOutput.contains("CompileFailed") ? 1 : 0;
//                        test += mvnOutput.contains("There are test failures") ? 1 : 0;
//                        checkstyle += mvnOutput.contains("Checkstyle violation") ||
//                                mvnOutput.contains("Failed during checkstyle execution") ? 1 : 0;
//                        dependency += mvnOutput.contains("Failed to collect dependencies") ? 1 : 0;
//                        pom += mvnOutput.contains("Non-resolvable import POM") ? 1 : 0;
//                        if(!(mvnOutput.contains("Compilation failure") || mvnOutput.contains("There are test failures")
//                                || mvnOutput.contains("Checkstyle violation") || mvnOutput.contains("Failed to collect dependencies")
//                                || mvnOutput.contains("Non-resolvable import POM") || mvnOutput.contains("Failed during checkstyle execution")
//                                || mvnOutput.contains("CompileFailed")))
//                            other++;
//                    }
                }
            }
        }

//        System.out.println("all " + all + " success " + success + " fail " + fail + " compilation " + compilation
//                + " test " + test + " checkstyle " + checkstyle + " dependency " + dependency + " pom " + pom);

        bestCase.entrySet().forEach(e -> {
            String rule = e.getKey();
            RuleRepairStat stat = e.getValue();
            int before = ruleToSuccessStat.get(rule).getNbBefore() + ruleToAllFailStat.get(rule).getNbBefore(),
                    fixed = ruleToSuccessStat.get(rule).getNbFixed() + ruleToAllFailStat.get(rule).getNbFixed()
                            - ruleToIrreparableFailStat.get(rule).getNbFixed(),
                    performed = ruleToSuccessStat.get(rule).getNbPerformed() + ruleToAllFailStat.get(rule).getNbPerformed()
                            - ruleToIrreparableFailStat.get(rule).getNbPerformed();

            stat.addStat(before, performed, before - fixed);
        });

        worstCase.entrySet().forEach(e -> {
            String rule = e.getKey();
            RuleRepairStat stat = e.getValue();
            int before = ruleToSuccessStat.get(rule).getNbBefore() + ruleToAllFailStat.get(rule).getNbBefore(),
                    fixed = ruleToSuccessStat.get(rule).getNbFixed(),
                    performed = ruleToSuccessStat.get(rule).getNbPerformed() + ruleToAllFailStat.get(rule).getNbPerformed();

            stat.addStat(before, performed, before - fixed);
        });

        System.out.println("BEST CASE TABLE");
        generateTable(bestCase, null);

        System.out.println("WORST CASE TABLE");
        generateTable(worstCase, null);


        int all = 0, cnt = 0;
        Map<String, Integer> oldRuleToAll = new HashMap<>();
        File tmp = new File("/home/khaes/phd/projects/sorald/tmp/tmp/Sorald-experiments/repair-results");
        for(File f : tmp.listFiles()){
            File json = f.toPath().resolve("repair-2111.json").toFile();
            if(!json.exists() || !consideredRepos.contains(f.getName()))
                continue;
            JSONObject jo = new JSONObject(FileUtils.readFileToString(json, "UTF-8"));
            JSONArray ja = (JSONArray) jo.get("repairs");
            if(ja.length() > 0) {
                jo = (JSONObject) ja.get(0);
                int nbBefore = jo.getInt("nbViolationsBefore");
                oldRuleToAll.put(f.getName(), nbBefore);
                if(!ruleToAll.containsKey(f.getName()) || nbBefore != ruleToAll.get(f.getName())) {
                    System.out.println(f.getName() + " " + (all += nbBefore) + " " + ruleToAll.get(f.getName()));
                    cnt++;
                }
            }
        }
        ruleToAll.entrySet().forEach(e -> {
            if(!oldRuleToAll.containsKey(e.getKey()))
                System.out.println(e.getKey() + " " + e.getValue());
        });
        System.out.println(cnt);
    }

    private static void generateTable(Map<String, RuleRepairStat> ruleToStat, Map<String, Integer> minedViolations){
        final int REPO_CNT = 161;

        String temp = "{ID} & {AOR}\\% \\hfill ({A}/{D}) & {FAR}\\% \\hfill ({F}/{A}) & {FDR}\\% \\hfill ({F}/{D}) & {FRT} & {GP}\\% ({G}/{GT}) & {T} \\\\";

        String[] CONSIDERED_RULES = new String[] {"1217", "1860", "2095", "2111", "2116", "2142", "2184", "2225", "2272", "4973"};
        Double[] medianTimes = new Double[] {4.5, 4.4, 6.3, 4.9, 4.5, 4.6, 4.5, 4.5, 4.5, 4.4};
        int[] failingRepos = new int[] {0, 0, 1, 2, 0, 0, 1, 1, 0, 2};
        int[] rt = new int[] {20, 15, 5, 5, 5, 15, 5, 5, 5, 5};


//        reader = new JsonReader(new FileReader("C:\\other\\daneshgah\\phd-kth\\projects\\sorald\\sorald-experiments\\sorald-results.json"));
//        reader = new JsonReader(new FileReader("C:\\other\\daneshgah\\phd-kth\\projects\\explanation generation\\Temp\\files\\sorald-results4.json"));
//        SoraldResults latestResults = gson.fromJson(reader, SoraldResults.class);

        int totalRt = 0;

        for(int i = 0; i < CONSIDERED_RULES.length; i++) {
            String rule = "S" + CONSIDERED_RULES[i];
            RuleRepairStat stats = ruleToStat.get(rule);

            String id = rule, d = (minedViolations != null ? minedViolations.get(rule) : stats.getNbBefore()) + "", a = stats.getNbPerformed() + "",
                    far = (stats.getNbFixed() * 100 / stats.getNbPerformed()) + "",
                    fdr = (stats.getNbFixed() * 100 / (minedViolations != null ? minedViolations.get(rule) : stats.getNbBefore())) + "",
                    f = stats.getNbFixed() + "", gp = (double) (failingRepos[i] * 1000 / REPO_CNT) / 10.0 + "",
                    g = failingRepos[i] + "", gt = REPO_CNT + "", t = medianTimes[i] + "",
                    aor = (stats.getNbPerformed() * 100 / (minedViolations != null ? minedViolations.get(rule) : stats.getNbBefore())) + "",
                    frt = (stats.getNbFixed() * rt[i]) + "";

            totalRt += stats.getNbFixed() * rt[i];

//            g = "0";
//            gp = "0.0";

            System.out.println(temp.replace("{ID}", id).replace("{D}", d).replace("{A}", a)
                    .replace("{FAR}", far).replace("{F}", f).replace("{FDR}", fdr)
                    .replace("{GP}", gp).replace("{G}", g).replace("{GT}", gt).replace("{T}", t)
                    .replace("{AOR}", aor).replace("{FRT}", frt));
        }

        System.out.println("\\midrule");

        int totalBefore = (minedViolations == null ? ruleToStat.entrySet().stream().mapToInt(e -> e.getValue().getNbBefore()).sum()
                : minedViolations.values().stream().mapToInt(Integer::intValue).sum()),
                totalFixed = ruleToStat.entrySet().stream().mapToInt(e -> e.getValue().getNbFixed()).sum(),
                totalPerformed = ruleToStat.entrySet().stream().mapToInt(e -> e.getValue().getNbPerformed()).sum(),
                totalFail = Arrays.stream(failingRepos).sum();
        String id = "ALL", d = totalBefore + "", a = totalPerformed + "",
                far = (totalFixed * 100 / totalPerformed) + "",
                fdr = (totalFixed * 100 / totalBefore) + "",
                f = totalFixed + "",
                gp = (double) (totalFail * 1000 / (REPO_CNT * 10)) / 10.0 + "",
                g = totalFail + "", gt = REPO_CNT * CONSIDERED_RULES.length + "",
                t = 0 + "",
                aor = (totalPerformed * 100 / totalBefore) + "",
                frt = totalRt + "";

//        g = "3";
//        gp = "1.8";

        System.out.println(temp.replace("{ID}", id).replace("{D}", d).replace("{A}", a)
                .replace("{FAR}", far).replace("{F}", f).replace("{FDR}", fdr)
                .replace("{GP}", gp).replace("{G}", g).replace("{GT}", gt).replace("{T}", t)
                .replace("{AOR}", aor).replace("{FRT}", frt));
    }
}

