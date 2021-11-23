import sys
import os
import multiprocessing as mp

rules = ["1217", "1860", "2095", "2111", "2116", "2142", "2184", "2225", "2272", "4973"]
#rules = ["2095"]

def process(repo, commit):
	for rule in rules:
		reponame = repo.split("/")[-1].split(".")[0]
		exec_id = reponame + "_" + rule
		print(f"Working on: {exec_id}")
		os.system(f"git clone {repo} {exec_id}")
		os.chdir(f"{exec_id}")
		os.system(f"git checkout {commit}")
		os.chdir("..")
		os.system(f"java -jar sorald.jar repair --source {exec_id} --rule-key {rule} --stats-output-file output2/{exec_id}_sorald_stats.json 1>> output2/{exec_id}_sorald.log 2>> output2/{exec_id}_sorald.err")
		os.chdir(f"{exec_id}")
		os.system(f"git diff > ../output2/{exec_id}_diff.diff")
		os.chdir("..")
		with open(f"output2/{exec_id}_sorald.log", 'r') as file:
			sorald_result = file.read()
		if not "No rule violations found" in sorald_result:
			os.chdir(f"{exec_id}")
			os.system(f"mvn test 1>> ../output2/{exec_id}_mvn.log 2>> ../output2/{exec_id}_mvn.err")
			os.chdir("..")
		else:
			print(f"Skipping {exec_id} because of no change from sorald")
		os.system(f"rm {exec_id} -rf")


def main(argv):
    pool = mp.Pool()
    
    with open(argv[0]) as file:
        lines = file.readlines()
        lines = [line.rstrip() for line in lines]
        for i in range(1, len(lines)):
            repo = lines[i].split(",")[0]
            commit = lines[i].split(",")[1]
            pool.apply_async(process, args = (repo, commit, ))

    pool.close()
    pool.join()

if __name__ == "__main__":
    main(sys.argv[1:])
