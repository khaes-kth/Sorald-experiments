import sys
import os
import multiprocessing as mp
import datetime
import subprocess


def current_readable_time():
	return datetime.datetime.now().strftime('%Y-%m-%d-%H-%M-%S')

rules = ["1217", "1860", "2095", "2111", "2116", "2142", "2184", "2225", "2272", "4973"]
#rules = ["2095"]

OUTPUT_DIR = f"output-{current_readable_time()}"
os.mkdir(f'{OUTPUT_DIR}')

def clone(repo, reponame, commit):
	os.system(f"rm {reponame} -rf")
	os.system(f"git clone {repo} {reponame}")
	subprocess.run(f"git checkout {commit}", cwd=reponame, shell=True)

def process(repo, commit):
	reponame = repo.split("/")[-1].split(".")[0]
	clone(repo, reponame, commit)
	os.system(f"java -jar sorald.jar mine --source {reponame} --handled-rules --rule-types BUG --stats-output-file {OUTPUT_DIR}/{reponame}_sorald_mine_stats.json 1>> {OUTPUT_DIR}/{reponame}_sorald_mine.log 2>> {OUTPUT_DIR}/{reponame}_sorald_mine.err")
#	os.system(f"java -jar sorald-old.jar mine --original-files-path {reponame} --handled-rules --rule-types BUG --stats-output-file {OUTPUT_DIR}/{reponame}_sorald_mine_stats.json 1>> {OUTPUT_DIR}/{reponame}_sorald_mine.log 2>> {OUTPUT_DIR}/{reponame}_sorald_mine.err")
	for rule in rules:
		exec_id = reponame + "_" + rule
		print(f"Working on: {exec_id}")
		clone(repo, reponame, commit)
		os.system(f"java -jar sorald.jar repair --source {reponame} --rule-key {rule} --stats-output-file {OUTPUT_DIR}/{exec_id}_sorald_repair_stats.json 1>> {OUTPUT_DIR}/{exec_id}_sorald_repair.log 2>> {OUTPUT_DIR}/{exec_id}_sorald_repair.err")
#		os.system(f"java -jar sorald-old.jar repair --original-files-path {reponame} --rule-keys {rule} --file-output-strategy IN_PLACE --stats-output-file {OUTPUT_DIR}/{exec_id}_sorald_repair_stats.json 1>> {OUTPUT_DIR}/{exec_id}_sorald_repair.log 2>> {OUTPUT_DIR}/{exec_id}_sorald_repair.err")
		subprocess.run(f"git diff > ../{OUTPUT_DIR}/{exec_id}_diff.diff", cwd=reponame, shell=True)
		with open(f"{OUTPUT_DIR}/{exec_id}_sorald_repair_stats.json", 'r') as file:
			sorald_result = file.read()
		if "nbViolationsBefore" in sorald_result:
			subprocess.run(f"mvn test 1>> ../{OUTPUT_DIR}/{exec_id}_mvn.log 2>> ../{OUTPUT_DIR}/{exec_id}_mvn.err", cwd=reponame, shell=True)
		else:
			print(f"Skipping {exec_id} because of no change from sorald")
		subprocess.run("git reset HEAD --hard", cwd=reponame, shell=True)


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
