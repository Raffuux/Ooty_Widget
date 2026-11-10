"""Builds app/src/main/assets/plan.json (for the widget) and index.html (full view) from the training Excel.
Usage: python3 tools/build_assets.py Planning_Ooty_2027.xlsx
"""
import openpyxl, json, sys, datetime, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'app', 'src', 'main', 'assets')
KEYS = {'Date':'date','Jour':'jour','Sem.':'sem','Phase':'phase','Course à pied':'course','Km cible':'km','Type':'type',
        'Kickboxing':'kick','Salle':'salle','Routine':'routine','Événement':'event','Fait ?':'fait','Km réalisé':'kmr',
        'D+ réalisé':'dpr','Tibias (/10)':'tib','Ressenti':'ressenti'}

def cell(v):
    return v.strftime('%Y-%m-%d') if isinstance(v, datetime.datetime) else v

wb = openpyxl.load_workbook(sys.argv[1])
today = datetime.date.today().isoformat()

# plan.json: one object per day of the Calendrier tab
ws = wb['Calendrier']
rows = [[cell(v) for v in r] for r in ws.iter_rows(values_only=True)]
h = next(i for i, r in enumerate(rows) if r and r[0] == 'Date')
hdr = rows[h]
days = []
for r in rows[h + 1:]:
    d = {KEYS[n]: v for n, v in zip(hdr, r) if n in KEYS and v not in (None, '')}
    if 'date' in d:
        days.append(d)
os.makedirs(ASSETS, exist_ok=True)
with open(os.path.join(ASSETS, 'plan.json'), 'w') as f:
    json.dump({'updated': today, 'days': days}, f, ensure_ascii=False, separators=(',', ':'))

# index.html: the dashboard page with the raw tabs embedded
sheets = {n: [[cell(v) for v in r] for r in wb[n].iter_rows(values_only=True)]
          for n in ['Coach', 'Calendrier', 'Séances', 'Mobilité', 'Vacances & imprévus'] if n in wb.sheetnames}
data = json.dumps({'sheets': sheets, 'source': 'Planning_Ooty_2027.xlsx', 'updated': today}, ensure_ascii=False, separators=(',', ':'))
tpl = open(os.path.join(ROOT, 'tools', 'template.html')).read()
page = tpl.replace('/*DATA*/null/*END*/', '/*DATA*/' + data + '/*END*/')
head = ('<!doctype html><html lang="fr"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">'
        '<style>footer .actions{display:none}</style></head><body>')
with open(os.path.join(ASSETS, 'index.html'), 'w') as f:
    f.write(head + page + '</body></html>')
print(f'{len(days)} jours -> {ASSETS}')
