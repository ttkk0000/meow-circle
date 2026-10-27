"""只读核对领养进度、协议内试养及接回与照护接线；不执行任何测试。"""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
def read(path):return (root/path).read_text()
shared='kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/'
ui='kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/'
models=read(shared+'AdoptionStatus.kt')
for guard in ['fun availableAdoptionActions','fun AdoptionBody.currentContract()','it.status == "pending"','party in it.acceptedBy','fun adoptionProgressTitle','fun AdoptionBody.careStageAvailable','"return_agreed"']:
    assert guard in models,guard
assert 'availableAdoptionActions(it,state.user?.id)' in read(shared+'MobileStore.kt')
native=read(ui+'AdoptionPages.kt')
for guard in ['AdoptionProgressPage','AdoptionConfirmationPage','body.currentContract()','contract.terms.responsibilities,contract.terms.trialUntil','f.discardAndReload()','f.adoptAdoptionVersion()','s.unconfirmedResult','pet_placement_conflict','returnPlanReview','summary(plan)']:
    assert guard in native,guard
care=read(ui+'CarePages.kt')
assert 'AdoptionProgressPage(store,s,record)' in care
assert 'f.confirm(label,returnPlanReview(plan,body))' in care
assert 'FormField(f,s,"trialTerms"' not in care
assert 'body.careStageAvailable(record.state)' in care
server=read('internal/mobile/rescue.go')
for guard in ['trial_requires_contract_revision','func carePostingOpen','!carePostingOpen(p, a, s.Now())','a.CompletedAt == nil','a.CompletedAt.Add(30*24*time.Hour)','allowed := actor == a.ApplicantID','s.collaborator(tx, "caretaker", adoptionID, actor)']:
    assert guard in server,guard
web=read('web-client/src/core/adoption.ts')
for guard in ['currentAdoptionContract','contract.status === "pending"','adoptionActionOptions','returnActionAllowed','plan.state === "effective"','integer(row.data.effective_return) === revision','careStageAvailable']:
    assert guard in web,guard
resource=read('web-client/src/features/ResourcePage.tsx')
for guard in ['<AdoptionProgress','<ReturnProgress','returnActionAllowed(r, owner, action, revision)']:
    assert guard in resource,guard
editor=read('web-client/src/features/WorkflowEditor.tsx')
for guard in ['<AdoptionActionReview','<ReturnActionReview','adoptionActionOptions(context.record, owner)','body.note !== terms.responsibilities','previousTrial','integer(prepared.body.revision)']:
    assert guard in editor,guard
evidence=root/'docs/design/figma-study/adoption-implementation-2026-10-27'
contexts=list(evidence.glob('*.json'))
assert len(contexts)==25
for path in contexts:
    value=json.loads(path.read_text())
    assert value['screenshot_inspected'] and value['verification']=='design_read_only_not_runtime'
    assert path.with_suffix('.png').is_file()
assert read('internal/mobile/adoption_care_stage_test.go').count('func Test')==3
assert read('kmp/shared/src/commonTest/kotlin/com/ttkk0000/meowcircle/client/AdoptionStatusTest.kt').count('@Test')==2
print('领养进度只读核对：25份设计上下文／原图、共用角色动作／协议内试养、双方交接与候补、完整接回核对及effective动作、试养私密照护与完成后30天窗口；3Go＋2KMP回归仅源码，未执行。')
