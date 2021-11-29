import sys
import os
import multiprocessing as mp

rules = ["1217", "1860", "2095", "2111", "2116", "2142", "2184", "2225", "2272", "4973"]

def process(rule, workingDir):
    print(f"Working on: {rule}")
    os.chdir(f"{workingDir}")
    os.system(f"rm -rf {rule}")
    os.system(f"mkdir {rule}")
    os.system(f"mkdir {rule}/workspace")
    os.system(f"mkdir {rule}/output")
    os.system(f"REPAIR_TOOL=SoraldBot GITHUB_OAUTH=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 SELECTED_COMMITS_PATH={workingDir}/selected-commits.csv CREATE_FORK=true SONAR_RULES={rule} WORKSPACE_FOLDER={workingDir}/{rule}/workspace OUTPUT_FOLDER={workingDir}/{rule}/output java -jar repairnator-realtime.jar 1>> {rule}/log.log 2>> {rule}/err.err")


def main(argv):
    pool = mp.Pool()
    
    for rule in rules:
        pool.apply_async(process, args = (rule, argv[0], ))

    pool.close()
    pool.join()

if __name__ == "__main__":
    main(sys.argv[1:])
