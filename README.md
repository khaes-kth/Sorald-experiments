# Sorald Experiments
This repository contains data related to the experiments that we conduct to assess [Sorald](https://github.com/SpoonLabs/sorald).

The structure of the repository is as follows:
- `top-rules.csv`: A list of SonarQube rules ordered by the number times they are violated in the dataset of [1].
- `considered_repos_stats.csv`: List of repositories that we consider for our experiments.
- `repair-results/`: A directory that contains data for executing Sorald on considered repositories (RQ1).
  - `new-rq1.zip` file inside this directory presents all the outputs of Sorald.
- `soraldbot-experiments/`: A directory containing data related to our experiments with SoraldBot (RQ2 and RQ3).
  - `selected-commits.csv `: Includes data related to monitored commits.
  - `new-soraldbot-results.csv `: Includes data related to generated patches (RQ2).
  - `PRs.csv`: The submitted pull requests (RQ3).
- `sorald-on-spongebugs-data/`: The results of running Sorald on the dataset from [2].
- `extra-data/bug-rules-analysis.csv`: The data related to analysis of SonarJava bug rules.


````
[1]: Marcilio, D., Bonifácio, R., Monteiro, E., Canedo, E., Luz, W., & Pinto, G. (2019, May). Are static analysis violations really fixed? a closer look at realistic usage of sonarqube. In 2019 IEEE/ACM 27th International Conference on Program Comprehension (ICPC) (pp. 209-219). IEEE.
[2]: Marcilio, D., Furia, C. A., Bonifacio, R., & Pinto, G. (2020). SpongeBugs: Automatically generating fix suggestions in response to static code analysis warnings. Journal of Systems and Software, 168, 110671.
````

# Sonar Rules Labelling
Two authors label all SonarJava bug rules following the definitions below:
- Fixable: we can design templates that remove the violation and implement the expected behavior. Note that we should know which template should be applied on each violation. If we have multiple templates but do not know which one would work for each violation, the rule is not fixable.
- Partially fixable: there is a strict subset of the violations of the rule that are fixable. We should also be able to determine if a given violation lies in this subset or not.
- Unfixable: neither fixable, nor partially fixable.

If the labels are not the same the conflict is resolved via discussion between the two participants. A third participant also labels the rule, when the first two participants do not reach a definitive decision.

The results of this experiment can be accessed [here](https://github.com/khaes-kth/Sorald-experiments/blob/master/extra-data/bug-rules-analysis.csv).
