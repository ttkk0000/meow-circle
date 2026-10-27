"""只读检查预约暂停合同与公共页面接线；不执行测试或应用。"""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
def read(path): return (root/path).read_text()
shared='kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/'
ui='kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/'
server=read('internal/mobile/appointments.go')
for guard in ['case "propose", "restore", "safety_repropose":','v.Status == "replaced"','v.SafetyHoldVersion, v.ResolutionNote = a.SafetyHoldVersion, in.Note','both_safety_reviews_required','specific safety review acknowledgement required','if held {','a.SafetyReason = in.Note','s.appointmentContextCurrent','s.appointmentAdmission','s.adult(tx, party)']:
    assert guard in server,guard
assert 'held && a.SafetyHoldVersion == 0 && action == "safety_repropose"' in server
contract=json.loads(read('docs/api/mobile-v2.openapi.json'))
assert 'post' in contract['paths']['/api/v2/appointments/{id}/safety_repropose']
for name,fields in [('Appointment',{'safety_reason','safety_hold_version'}),('AppointmentVersion',{'safety_hold_version','resolution_note','safety_checks'})]:
    assert fields <= contract['components']['schemas'][name]['properties'].keys()
native=read(ui+'AppointmentPages.kt')
for guard in ['AppointmentCancelPage','s.unconfirmedResult','FeaturePage.Recovery','cancelled-${result.version}','record.version < resultVersion','f::submitConfirmation','record.canConfirmAppointment(actor)','record.safetyResolved()','v.status == "replaced"','f.adoptAppointmentVersion()']:
    assert guard in native,guard
care=read(ui+'CarePages.kt')
assert 'FeaturePage.Appointment -> AppointmentDetailPage(store,s,route)' in care
assert '"safety_repropose" else "propose"' in care
assert 'canConfirmAppointment' in read(shared+'AppointmentStatus.kt')
web=read('web-client/src/features/AppointmentPages.tsx')
for guard in ['AppointmentCancelTask','appointmentSafetyResolved(row)','canConfirmAppointment(row, actor)','version.status === "replaced"','查询本次原请求','baseVersion']:
    assert guard in web,guard
assert '<AppointmentDetail row={r} actor={owner}' in read('web-client/src/features/ResourcePage.tsx')
editor=read('web-client/src/features/WorkflowEditor.tsx')
assert '<AppointmentCancelTask' in editor and '<AppointmentVersionReview' in editor
assert 'context.record?.state === "safety_hold"' in editor
tests=read('internal/mobile/appointment_safety_test.go')
assert tests.count('func Test')==4
print('预约暂停只读核对：按暂停版本重新协商／双方具体核对与资格、拒绝撤回到期保持暂停、历史恢复限制、三端取消四状态与原请求恢复接线；4Go＋2KMP回归仅源码，未执行。')
