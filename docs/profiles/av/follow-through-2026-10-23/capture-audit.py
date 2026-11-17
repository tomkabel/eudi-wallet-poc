from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, os, shlex, subprocess, time
root=Path.cwd()
out=root/'docs/profiles/av/follow-through-2026-10-23'
def fingerprint(path):
    data=path.read_bytes()
    return {'sha256':hashlib.sha256(data).hexdigest(),'size_bytes':len(data)}
# Pin runner, parser, reporting code, tests, all Go source, vectors and profile inputs.
paths={root/p for p in ('tests/run_av_conformance.py','tests/av_report.py','tests/check_av_registry.py','tests/test_av_conformance.py','tests/requirements-ci.txt','Makefile','verifier/go/go.mod','verifier/go/iso.html','verifier/zkverify-ffi/longfellow-rev.txt','verifier/zkverify-ffi/.cargo/config.toml')}
paths.update((root/'verifier/go').rglob('*.go'))
for base in ('tests/vectors/av-profile','tests/vectors/carrier-v1','verifier/go/zk/testdata'):
    paths.update(p for p in (root/base).rglob('*') if p.is_file())
paths.update(p for p in (root/'docs/profiles/av').iterdir() if p.is_file() and p.name not in ('report.json','report.html','reproduction-record.json','reproduction-transcript.txt'))
inputs={p.relative_to(root).as_posix():fingerprint(p) for p in sorted(paths)}
historical_names=('report.json','report.html','reproduction-record.json','reproduction-transcript.txt')
historical={name:fingerprint(root/'docs/profiles/av'/name) for name in historical_names}
patch_command=['git','diff','--binary','HEAD','--','tests/run_av_conformance.py','tests/test_av_conformance.py']
(out/'implementation.patch').write_bytes(subprocess.check_output(patch_command,cwd=root))
cmd=['python3','tests/run_av_conformance.py','--json','docs/profiles/av/follow-through-2026-10-23/provisional-agent-local-report.json','--html','docs/profiles/av/follow-through-2026-10-23/provisional-agent-local-report.html']
env=os.environ.copy(); env['JAVA_HOME']='/usr/lib/jvm/java-17-openjdk'; env['PATH']=env['JAVA_HOME']+'/bin:'+env['PATH']; env['RUSTFLAGS']='-C target-cpu=x86-64-v3'
start=datetime.now(timezone.utc).isoformat(); clock=time.monotonic()
proc=subprocess.run(cmd,cwd=root,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180)
seconds=round(time.monotonic()-clock,3); end=datetime.now(timezone.utc).isoformat()
transcript=f'Agent-local provisional workspace audit; independent reproduction UNPERFORMED.\nCWD: {root}\nEnvironment override: JAVA_HOME={env["JAVA_HOME"]}; PATH prepends $JAVA_HOME/bin; RUSTFLAGS={env["RUSTFLAGS"]}\nCommand: {shlex.join(cmd)}\nStart UTC: {start}\n'.encode()+proc.stdout+f'\nReturn code: {proc.returncode}\nEnd UTC: {end}\nElapsed seconds: {seconds}\n'.encode()
(out/'audit-transcript.txt').write_bytes(transcript)
report=json.loads((out/'provisional-agent-local-report.json').read_text()); old=json.loads((root/'docs/profiles/av/report.json').read_text())
def semantics(r):
    return {'profile_revision':r['profile']['revision'],'registry':r['registry'], 'summary':r['summary'],'audit_exit_code':r['audit_exit_code'], 'candidates':[{'name':c['name'],'counts':c['counts'],'ordered_clauses':[{k:row[k] for k in ('clause_id','test_id','state','scope','limits') if k in row} for row in c['clauses']], 'exclusions':[(row['clause_id'],row['detail']) for row in c['clauses'] if row['state']=='EXCLUDED'], 'negative_results':c.get('negative_results',[])} for c in r['candidates']]}
comparison=semantics(report)==semantics(old)
assert comparison, 'historical semantic counts/states/mappings changed'
assert proc.returncode==report['audit_exit_code']==1
assert report['repository']['dirty'] is True
assert report['summary']['environment_errors']==0
assert report['external_reproduction']['state']=='UNPERFORMED'
assert all(fingerprint(root/name)==value for name,value in inputs.items()), 'audit inputs changed during capture'
assert all(fingerprint(root/'docs/profiles/av'/name)==value for name,value in historical.items()), 'historical evidence changed'
details=[{'candidate':c['name'],'clause_id':row['clause_id'],'historical_detail':prior['detail'],'current_detail':row['detail']} for c,old_c in zip(report['candidates'],old['candidates']) for row,prior in zip(c['clauses'],old_c['clauses']) if row.get('detail')!=prior.get('detail')]
html=(out/'provisional-agent-local-report.html').read_text()
assert html.count('<article')==188 and 'dirty at audit start: True' in html and 'Audit exit: 1' in html
assert report['repository']['revision'] in html and 'provisional-agent-local-report.json' in html
verification='Command: python3 docs/profiles/av/follow-through-2026-10-23/capture-audit.py\nPASS: '+str(len(inputs))+' input fingerprints captured before audit and unchanged after audit.\nPASS: six retained artifacts comprise provisional JSON/HTML, audit transcript, source delta, capture procedure and this verification transcript.\nPASS: four historical artifact hashes unchanged.\nPASS: pinned profile/registry, summary, ordered clause ids/test ids/states/scope/limits, exclusion reasons and named negative states match historical.\nPASS: 188 HTML rows, revision/dirty/verdict identity and provisional JSON sibling name.\nMapped clause detail differences are retained in audit-record.json without normalizing them away.\n'
(out/'verification-transcript.txt').write_text(verification)
artifacts={name:fingerprint(out/name) for name in ('provisional-agent-local-report.json','provisional-agent-local-report.html','audit-transcript.txt','implementation.patch','capture-audit.py','verification-transcript.txt')}
record={'kind':'agent-local provisional dirty-workspace audit; independent reproduction UNPERFORMED; not externally published','base_revision':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'repository_dirty_at_audit_start':report['repository']['dirty'],'start_utc':start,'end_utc':end,'seconds':seconds,'command':cmd,'cwd':str(root),'environment_overrides':{'JAVA_HOME':env['JAVA_HOME'],'PATH_prefix':env['JAVA_HOME']+'/bin','RUSTFLAGS':env['RUSTFLAGS']},'returncode':proc.returncode,'inputs':inputs,'input_scope':'Runner/report/parser/regression code, pinned Python requirements, Makefile/FFI configuration, Go module, embedded iso.html and all Go source, retained AV/carrier/Go fixture files and profile/source registry inputs. Hashes captured before audit and verified unchanged after it. Existing compiled staticlib fingerprint and effective toolchain/environment are in provisional-agent-local-report.json; this is not a hermetic build or historical compiler attestation.','artifacts':artifacts,'source_delta':{'command':patch_command,'base_revision':report['repository']['revision'],'path':'implementation.patch','restore':'At the exact base revision, git apply implementation.patch restores the changed runner/test inputs; verify all input hashes. The patch does not include user planning work.','limit':'Source/fixture inputs are recoverable from the base plus this patch; the compiled staticlib remains a separately fingerprinted existing artifact and tools/build environment are not snapshotted.'},'historical_evidence':{'path':'../','unchanged':True,'base_revision':old['repository']['revision'],'artifacts':historical},'semantic_comparison':{'matches_historical':comparison,'fields_compared':'Pinned profile/registry; full summary and audit exit; candidate names/counts; every ordered clause id/test id/state/scope/limits; named negative-control result states. Execution timing, host, invocation and checkout changes are provenance variations. Actual changed clause-detail text is retained separately below; map-driven subtest ordering and Go duration text are not clause verdict changes.','observed_clause_detail_differences':details,'current_summary':report['summary'],'current_candidate_counts':[{'name':c['name'],'counts':c['counts']} for c in report['candidates']]},'acceptance':{'story_1_3':'awaiting-operator: independent candidate and recorded revision/evidence absent','story_1_6':'awaiting-operator: publication and independent clean-checkout reproduction absent','clean_checkout_reproduction':'NOT_PERFORMED for these uncommitted fixes; historical clean evidence is preserved separately','operator_actions':['Review and commit the hardened implementation; regenerate from that clean full revision and publish through fork.','Execute an independent wallet or verifier at a recorded revision using the documented adapter; retain every mapped outcome.','Have an independent human or organization run the clean-checkout guide in one sitting; retain attribution, exact commands, return codes, timings, host, transcript and both reports.','Review and publish discrepancies and attributed evidence before accepting Stories 1.3 and 1.6.']}}
(out/'audit-record.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps({'returncode':proc.returncode,'seconds':seconds,'input_files':len(inputs),'semantic_match':comparison,'summary':report['summary'],'artifacts':artifacts},indent=2))
