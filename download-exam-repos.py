import requests
import os

# https://docs.github.com/en/rest/repos/repos?list-repositories-for-a-user
url = 'https://api.github.com/users/dancojocar/repos?per_page=100&page'

page = 1
while True:
    print(f'page: {page}')
    response = requests.get(f'{url}={page}')

    if response.status_code != 200:
        print(f'FAILED. response: {response}')
        exit(1)

    repos = response.json()
    if len(repos) == 0:
        break
    for i, repo in enumerate(repos):
        print(f'page: {page} repo: {i + 1}/{len(repos)}')
        repo_name = repo["name"]
        repo_url = repo["clone_url"]

        os.system(f'git clone --quiet {repo_url} exams/{repo_name}')

    page += 1

# find exams -type d -name ".git" -exec rm -rf {} \;
