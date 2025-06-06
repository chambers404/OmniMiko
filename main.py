import os
import json
import argparse
from http.server import SimpleHTTPRequestHandler, HTTPServer

SUPPLY_DIR = os.path.join(os.path.dirname(__file__), 'SupplyCache')
SESSION_DIR = os.path.join(os.path.dirname(__file__), 'Omnntmko_sess')
SESSION_FILE = os.path.join(SESSION_DIR, 'session.json')

def load_ascii_art(name):
    path = os.path.join(SUPPLY_DIR, name)
    if os.path.exists(path):
        with open(path, 'r') as f:
            return f.read()
    return ''

def store_session(data):
    os.makedirs(SESSION_DIR, exist_ok=True)
    with open(SESSION_FILE, 'w') as f:
        json.dump(data, f)

def load_session():
    if os.path.exists(SESSION_FILE):
        with open(SESSION_FILE, 'r') as f:
            return json.load(f)
    return {}

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=SUPPLY_DIR, **kwargs)


def start_server(port=8080):
    server = HTTPServer(('localhost', port), Handler)
    print(f'Serving GUI at http://localhost:{port}')
    server.serve_forever()


def ask_yes_no(prompt):
    yes = {'y', 'ye', 'yes', 'sure', 'yea', 'ok'}
    no = {'n', 'no', 'nah', 'nope'}
    while True:
        ans = input(prompt + ' ').strip().lower()
        if ans in yes:
            return True
        if ans in no:
            return False
        print('Please respond with yes or no.')

def run_cli():
    parser = argparse.ArgumentParser(description='OmniMiko CLI')
    parser.add_argument('command', nargs='*', help='Command to run')
    parser.add_argument('--server', action='store_true', help='Start GUI server')
    args = parser.parse_args()

    if args.server:
        start_server()
        return

    if args.command:
        os.system(' '.join(args.command))
        return

    print(load_ascii_art('welcome.txt'))
    if ask_yes_no('Store ssh login for 24 hours?'):
        store_session({'ssh_stored': True})
        print('Session stored.')
    else:
        print('Session not stored.')

if __name__ == '__main__':
    run_cli()
