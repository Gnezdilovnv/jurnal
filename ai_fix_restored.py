import os, sys, re, json, requests, time

with open('.env', 'r') as f:
    env = {}
    for line in f:
        if '=' in line:
            k, v = line.strip().split('=', 1)
            env[k] = v

DEEPSEEK_KEY = env.get('DEEPSEEK_API_KEY')

if not DEEPSEEK_KEY:
    print("❌ DeepSeek ключ не найден")
    sys.exit(1)

def find_kotlin_files(root_dir):
    files = []
    for root, dirs, filenames in os.walk(root_dir):
        dirs[:] = [d for d in dirs if d not in ['.git', 'build', '.idea', '.gradle', 'libs', 'generated']]
        for filename in filenames:
            if filename.endswith('.kt'):
                files.append(os.path.join(root, filename))
    return files

def read_file(path):
    try:
        with open(path, 'r', encoding='utf-8') as f:
            return f.read()
    except:
        return None

def fix_file(path, content):
    try:
        resp = requests.post(
            'https://api.deepseek.com/v1/chat/completions',
            headers={'Authorization': f'Bearer {DEEPSEEK_KEY}'},
            json={
                'model': 'deepseek-coder',
                'messages': [
                    {'role': 'system', 'content': 'Исправь ошибки. Верни ТОЛЬКО исправленный код.'},
                    {'role': 'user', 'content': f'Файл: {path}\n\n```kotlin\n{content}\n```'}
                ],
                'max_tokens': 3000,
                'temperature': 0.2
            },
            timeout=90
        )
        if resp.status_code == 200:
            fixed = resp.json()['choices'][0]['message']['content']
            fixed = re.sub(r'```kotlin\s*', '', fixed)
            fixed = re.sub(r'```\s*', '', fixed)
            return fixed.strip()
        return None
    except Exception as e:
        return None

def save_file(path, content):
    try:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    except:
        return False

print('='*60)
print('📂 ПОИСК ФАЙЛОВ')
print('='*60)

kotlin_files = find_kotlin_files('app/src/main/java')
print(f'📋 Найдено Kotlin файлов: {len(kotlin_files)}')

if not kotlin_files:
    print('❌ Файлы не найдены!')
    sys.exit(1)

print('\n📂 НАЙДЕННЫЕ ФАЙЛЫ:')
for f in kotlin_files:
    print(f'  📄 {f}')

print('\n' + '='*60)
print('🔍 ИСПРАВЛЕНИЕ ФАЙЛОВ')
print('='*60)

fixed_files = []

for i, file_path in enumerate(kotlin_files, 1):
    print(f'\n📄 [{i}/{len(kotlin_files)}] {file_path}')
    print('-'*40)
    
    content = read_file(file_path)
    if not content:
        print('  ❌ Не удалось прочитать')
        continue
    
    print(f'  📏 Размер: {len(content)} символов')
    print('  🔧 Исправление...')
    
    fixed = fix_file(file_path, content)
    if fixed and fixed != content:
        if save_file(file_path, fixed):
            fixed_files.append(file_path)
            print('  ✅ Исправлен!')
        else:
            print('  ❌ Ошибка сохранения')
    else:
        print('  ✅ Ошибок нет')
    
    time.sleep(0.5)

print('\n' + '='*60)
print(f'✅ Исправлено файлов: {len(fixed_files)}')
if fixed_files:
    print('\n📝 Исправленные файлы:')
    for f in fixed_files:
        print(f'  - {f}')
