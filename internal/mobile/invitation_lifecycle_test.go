package mobile_test

import (
	"context"
	"errors"
	"fmt"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// 回归源码：本批不执行编译或测试。
func collaborationInvite(t *testing.T, s *mobile.Service, owner, recipient, target int64, scope string) mobile.Record {
	t.Helper()
	r, e := s.Invite(context.Background(), owner, mobile.InviteInput{CommandMeta: meta(), Scope: scope, TargetID: target, UserID: recipient})
	if e != nil {
		t.Fatal(e)
	}
	return r
}

func removeInteractionPhone(t *testing.T, repo mobile.Repository, actor int64) {
	t.Helper()
	if e := repo.Update(context.Background(), func(tx mobile.Tx) error {
		u, e := tx.User(actor)
		if e != nil {
			return e
		}
		u.Phone = ""
		return tx.SaveUser(&u)
	}); e != nil {
		t.Fatal(e)
	}
}

func TestInvitationRechecksCurrentOwnerAndAllowsNewOwnerToReinvite(t *testing.T) {
	for _, accepted := range []bool{false, true} {
		t.Run(fmt.Sprint("accepted=", accepted), func(t *testing.T) {
			s, repo := fixture(t)
			ctx := context.Background()
			p := pet(t, s, true)
			i := collaborationInvite(t, s, 1, 4, p.ID, "adoption_editor")
			if accepted {
				var e error
				i, e = s.Invitation(ctx, 4, i.ID, i.Version, "accept")
				if e != nil {
					t.Fatal(e)
				}
			}
			p.OwnerID = 3
			p = serverRecord(t, repo, mobile.Pets, p, body[mobile.Pet](t, p))
			if !accepted {
				if _, e := s.Invitation(ctx, 4, i.ID, i.Version, "accept"); !errors.Is(e, mobile.ErrForbidden) {
					t.Fatal("旧主人邀请不应在归属变更后接受", e)
				}
			} else if _, e := s.Introduction(ctx, 4, p.ID, mobile.IntroductionInput{CommandMeta: meta(), Version: p.Version, Bio: "旧授权不能修改"}); !errors.Is(e, mobile.ErrForbidden) {
				t.Fatal("旧主人授权仍可写入", e)
			}
			i = collaborationInvite(t, s, 3, 4, p.ID, "adoption_editor")
			if _, e := s.Invitation(ctx, 4, i.ID, i.Version, "accept"); e != nil {
				t.Fatal("旧授权不应永久阻止现主人重新授权", e)
			}
		})
	}
}

func TestBlockingInvalidatesPendingAndAcceptedCollaboration(t *testing.T) {
	for _, accepted := range []bool{false, true} {
		t.Run(fmt.Sprint("accepted=", accepted), func(t *testing.T) {
			s, _ := fixture(t)
			ctx := context.Background()
			p := pet(t, s, true)
			i := collaborationInvite(t, s, 1, 4, p.ID, "adoption_editor")
			if accepted {
				var e error
				i, e = s.Invitation(ctx, 4, i.ID, i.Version, "accept")
				if e != nil {
					t.Fatal(e)
				}
			}
			if _, e := s.Friend(ctx, 1, "block", mobile.FriendInput{CommandMeta: meta(), UserID: 4}); e != nil {
				t.Fatal(e)
			}
			var e error
			if accepted {
				_, e = s.Introduction(ctx, 4, p.ID, mobile.IntroductionInput{CommandMeta: meta(), Version: p.Version, Bio: "拉黑后不应继续协作"})
			} else {
				_, e = s.Invitation(ctx, 4, i.ID, i.Version, "accept")
			}
			if !errors.Is(e, mobile.ErrForbidden) {
				t.Fatal("拉黑未使协作失效", e)
			}
		})
	}
}

func TestDeclineRevokeAndLeaveRemainAvailableWithoutInteraction(t *testing.T) {
	for _, action := range []string{"decline", "revoke", "leave"} {
		t.Run(action, func(t *testing.T) {
			s, repo := fixture(t)
			ctx := context.Background()
			p := pet(t, s, true)
			i := collaborationInvite(t, s, 1, 4, p.ID, "adoption_editor")
			actor := int64(4)
			if action == "leave" {
				var e error
				i, e = s.Invitation(ctx, 4, i.ID, i.Version, "accept")
				if e != nil {
					t.Fatal(e)
				}
			} else if action == "revoke" {
				actor = 1
			} else {
				v := body[mobile.Invitation](t, i)
				v.ExpiresAt = s.Now().Add(-time.Hour)
				i = serverRecord(t, repo, mobile.Invitations, i, v)
			}
			removeInteractionPhone(t, repo, actor)
			ctx = mobile.WithCommandID(ctx, meta().ClientRequestID)
			result, e := s.Invitation(ctx, actor, i.ID, i.Version, action)
			if e != nil {
				t.Fatal("安全结束操作被互动资格拦截", action, e)
			}
			again, e := s.Invitation(ctx, actor, i.ID, i.Version, action)
			if e != nil || again.ID != result.ID || again.Version != result.Version || again.State != result.State {
				t.Fatal("原请求重放没有返回原操作结果", again, e)
			}
			if _, e = s.Invitation(mobile.WithCommandID(context.Background(), meta().ClientRequestID), actor, i.ID, i.Version, action); !errors.Is(e, mobile.ErrConflict) {
				t.Fatal("新请求绕过原版本", e)
			}
		})
	}
}

func TestCareContextSupportsTrialButNeverExposesApplicationToCollaborator(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	a := closureApplication(t, s, repo, p.ID, 2)
	v := body[mobile.Adoption](t, a)
	v.TrialConfirmations = []int64{1, 2}
	a.State = "trial_active"
	a = serverRecord(t, repo, mobile.Adoptions, a, v)
	i := collaborationInvite(t, s, 2, 4, a.ID, "caretaker")
	i, e := s.Invitation(ctx, 4, i.ID, i.Version, "accept")
	if e != nil {
		t.Fatal(e)
	}
	photo := serverRecord(t, repo, mobile.Media, mobile.Record{OwnerID: 2, State: "ready"}, mobile.MediaFile{Scope: "private", MIME: "image/png"})
	care, e := s.Care(ctx, 2, a.ID, mobile.CareInput{CommandMeta: meta(), Text: "正常进食", MediaIDs: []int64{photo.ID}})
	if e != nil {
		t.Fatal(e)
	}
	context, e := s.CareContext(ctx, 4, a.ID)
	if e != nil || !context.CanPost || context.AdoptionID != a.ID || context.State != "trial_active" {
		t.Fatal(context, e)
	}
	if _, e = s.Get(ctx, 4, mobile.Adoptions, a.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("照护协作者读到了完整申请与协议", e)
	}
	if _, e = s.CareContext(ctx, 3, a.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("无关用户读到了照护上下文", e)
	}
	publisher, e := s.CareContext(ctx, 1, a.ID)
	if e != nil || publisher.CanPost || publisher.Reason != "care_recipient_required" {
		t.Fatal("发布方获得了接收方近况写入入口", publisher, e)
	}
	if _, e = s.Get(ctx, 4, mobile.CareUpdates, care.ID); e != nil {
		t.Fatal(e)
	}
	if _, e = s.Get(ctx, 4, mobile.Media, photo.ID); e != nil {
		t.Fatal("有效照护授权不能读所附私密照片", e)
	}
	// 接受截止不是已接受授权的自动失效日；写窗口单独由阶段决定。
	inviteBody := body[mobile.Invitation](t, i)
	inviteBody.ExpiresAt = s.Now().Add(-time.Hour)
	i = serverRecord(t, repo, mobile.Invitations, i, inviteBody)
	context, e = s.CareContext(ctx, 4, a.ID)
	if e != nil || !context.CanPost {
		t.Fatal("已接受授权被接受截止错误终止", context, e)
	}
	removeInteractionPhone(t, repo, 4)
	context, e = s.CareContext(ctx, 4, a.ID)
	if e != nil || context.CanPost || context.Reason != "interaction_required" {
		t.Fatal("上下文没有反映当前互动资格", context, e)
	}
	closed := s.Now().Add(-31 * 24 * time.Hour)
	a.State, v.CompletedAt = "completed", &closed
	a = serverRecord(t, repo, mobile.Adoptions, a, v)
	context, e = s.CareContext(ctx, 4, a.ID)
	if e != nil || context.CanPost || context.Reason != "care_stage_closed" {
		t.Fatal("照护写入窗口没有关闭", context, e)
	}
	if _, e = s.Get(ctx, 4, mobile.CareUpdates, care.ID); e != nil {
		t.Fatal("写窗口结束不应删除已授权的历史读取权", e)
	}
	leave := mobile.WithCommandID(ctx, meta().ClientRequestID)
	if _, e = s.Invitation(leave, 4, i.ID, i.Version, "leave"); e != nil {
		t.Fatal(e)
	}
	if _, e = s.CareContext(ctx, 4, a.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("退出后仍可读取上下文", e)
	}
	if _, e = s.Get(ctx, 4, mobile.CareUpdates, care.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("退出后仍可读取他人照护近况", e)
	}
	if _, e = s.Get(ctx, 4, mobile.Media, photo.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("退出后仍可读取他人私密照片", e)
	}
	if _, e = s.Invitation(leave, 4, i.ID, i.Version, "leave"); e != nil {
		t.Fatal("退出回放失败", e)
	}
	if _, e = s.Get(ctx, 4, mobile.CareUpdates, care.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("退出回放恢复了协作权限", e)
	}
}

func TestCaretakerCannotAcceptAfterPostingStageCloses(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	p := pet(t, s, true)
	a := closureApplication(t, s, repo, p.ID, 2)
	v := body[mobile.Adoption](t, a)
	v.TrialConfirmations = []int64{1, 2}
	a.State = "trial_active"
	a = serverRecord(t, repo, mobile.Adoptions, a, v)
	i := collaborationInvite(t, s, 2, 4, a.ID, "caretaker")
	a.State = "returned"
	serverRecord(t, repo, mobile.Adoptions, a, v)
	if _, e := s.Invitation(ctx, 4, i.ID, i.Version, "accept"); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatal("已结束照护阶段仍可接受旧邀请", e)
	}
	if _, e := s.CareContext(ctx, 4, a.ID); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("拒绝的旧邀请仍授予读取权", e)
	}
}

func TestRevokingPendingGroupInviteDoesNotRemoveIndependentlyJoinedMember(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	g, e := s.CreateGroup(ctx, 1, mobile.GroupInput{CommandMeta: meta(), Name: "猫咪照护交流"})
	if e != nil {
		t.Fatal(e)
	}
	i := collaborationInvite(t, s, 1, 2, g.ID, "group_member")
	v := body[mobile.Group](t, g)
	v.Members = append(v.Members, 2)
	g.Participants = v.Members
	g = serverRecord(t, repo, mobile.Groups, g, v)
	if _, e = s.Invitation(ctx, 1, i.ID, i.Version, "revoke"); e != nil {
		t.Fatal(e)
	}
	current, e := s.Get(ctx, 2, mobile.Groups, g.ID)
	if e != nil || !mobile.Has(body[mobile.Group](t, current).Members, 2) {
		t.Fatal("撤销尚未接受的邀请移除了现有成员", current, e)
	}
}

func TestRevokingAcceptedGroupInviteClearsMuteAndTransferProposal(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	g, e := s.CreateGroup(ctx, 1, mobile.GroupInput{CommandMeta: meta(), Name: "猫咪照护交流"})
	if e != nil {
		t.Fatal(e)
	}
	i := collaborationInvite(t, s, 1, 2, g.ID, "group_member")
	i, e = s.Invitation(ctx, 2, i.ID, i.Version, "accept")
	if e != nil {
		t.Fatal(e)
	}
	g, e = s.Get(ctx, 1, mobile.Groups, g.ID)
	if e != nil {
		t.Fatal(e)
	}
	v := body[mobile.Group](t, g)
	until := s.Now().Add(time.Hour)
	v.Muted, v.PendingTransferTo, v.TransferExpiresAt = []int64{2}, 2, &until
	serverRecord(t, repo, mobile.Groups, g, v)
	if _, e = s.Invitation(ctx, 1, i.ID, i.Version, "revoke"); e != nil {
		t.Fatal(e)
	}
	g, e = s.Get(ctx, 1, mobile.Groups, g.ID)
	if e != nil {
		t.Fatal(e)
	}
	v = body[mobile.Group](t, g)
	if mobile.Has(v.Members, 2) || mobile.Has(v.Muted, 2) || v.PendingTransferTo != 0 || v.TransferExpiresAt != nil || mobile.Has(g.Participants, 2) {
		t.Fatal("移除成员未清理附属权限", v)
	}
}

func TestGroupRequestLimitAndOwnershipTransferScanBeyondFirstPage(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	g, e := s.CreateGroup(ctx, 1, mobile.GroupInput{CommandMeta: meta(), Name: "同城猫咪", Discoverable: true, City: "武汉", Summary: "照护交流"})
	if e != nil {
		t.Fatal(e)
	}
	v := body[mobile.Group](t, g)
	v.Members = append(v.Members, 2)
	g.Participants = v.Members
	g = serverRecord(t, repo, mobile.Groups, g, v)
	var expired mobile.Record
	for n := 0; n < 110; n++ {
		// 普通邀请和过期申请都不占有效待审名额，且排在真实申请之前。
		serverRecord(t, repo, mobile.Invitations, mobile.Record{OwnerID: 1, Participants: []int64{4}, RefID: g.ID, Key: fmt.Sprintf("fixture-member-%d", n), State: "pending"}, mobile.Invitation{Scope: "group_member", TargetID: g.ID, RecipientID: 4, ExpiresAt: s.Now().Add(time.Hour)})
		expired = serverRecord(t, repo, mobile.Invitations, mobile.Record{OwnerID: 4, Participants: []int64{1}, RefID: g.ID, Key: fmt.Sprintf("fixture-expired-%d", n), State: "pending"}, mobile.Invitation{Scope: "group_request", TargetID: g.ID, RecipientID: 1, ExpiresAt: s.Now().Add(-time.Hour)})
	}
	pending, e := s.RequestGroupJoin(ctx, 3, g.ID, mobile.GroupJoinInput{CommandMeta: meta(), Note: "希望参加照护交流"})
	if e != nil {
		t.Fatal("过期或普通邀请占用了待审额度", e)
	}
	g, e = s.Group(ctx, 1, g.ID, "transfer", mobile.GroupAction{CommandMeta: meta(), Version: g.Version, UserID: 2})
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Group(ctx, 2, g.ID, "accept_transfer", mobile.GroupAction{CommandMeta: meta(), Version: g.Version}); e != nil {
		t.Fatal(e)
	}
	for _, id := range []int64{pending.ID, expired.ID} {
		if _, e = s.Get(ctx, 1, mobile.Invitations, id); !errors.Is(e, mobile.ErrNotFound) {
			t.Fatal("旧群主保留了待审申请资料", id, e)
		}
		if _, e = s.Get(ctx, 2, mobile.Invitations, id); e != nil {
			t.Fatal("新群主未收到后页申请", id, e)
		}
	}
	pending, e = s.Get(ctx, 2, mobile.Invitations, pending.ID)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = s.Invitation(ctx, 2, pending.ID, pending.Version, "accept"); e != nil {
		t.Fatal(e)
	}
	if _, e = s.Get(ctx, 3, mobile.Groups, g.ID); e != nil {
		t.Fatal("有效申请未加入群聊", e)
	}
}

func TestInboxPendingInvitationsExcludeExpiredAndOutgoing(t *testing.T) {
	s, repo := fixture(t)
	for n, expiry := range []time.Time{s.Now().Add(-time.Second), s.Now().Add(time.Hour)} {
		serverRecord(t, repo, mobile.Invitations, mobile.Record{OwnerID: 1, Participants: []int64{2}, Key: fmt.Sprintf("fixture-inbox-%d", n), State: "pending"}, mobile.Invitation{Scope: "group_member", RecipientID: 2, ExpiresAt: expiry})
	}
	u, e := s.Unread(context.Background(), 2)
	if e != nil || u.Pending["invitations"] != 1 {
		t.Fatal("过期邀请计入待办", u, e)
	}
	u, e = s.Unread(context.Background(), 1)
	if e != nil || u.Pending["invitations"] != 0 {
		t.Fatal("已发出邀请计入待处理", u, e)
	}
}
