package identity_test

import (
	"context"
	"errors"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"testing"
)

type fixedOfficialProvider struct{}

func (fixedOfficialProvider) Verify(context.Context, identity.Credential, string) (identity.Subject, error) {
	return identity.Subject{Provider: "qq", AppID: "fixture-app", ID: "fixture-official-subject"}, nil
}

// 仅回归源码；当前任务不运行测试。冲突不透露另一账号资料，也不覆盖原绑定。
func TestProviderConflictPreservesOriginalIdentityAndNeedsExplicitCreation(t *testing.T) {
	s, _ := identityFixture()
	s.Providers = map[string]identity.Provider{"qq": fixedOfficialProvider{}}
	ctx := context.Background()
	ticket, err := s.Start(ctx, 0, identity.OAuthStart{Provider: "qq"})
	if err != nil {
		t.Fatal(err)
	}
	result, err := s.Finish(ctx, 0, identity.OAuthFinish{State: ticket.State, Credential: identity.Credential{Code: "fixture"}})
	if err != nil || result.Login != nil || !result.NeedsAccountChoice || result.ResolutionToken == "" {
		t.Fatal("new identity bypassed account choice", err)
	}
	login, err := s.Resolve(ctx, 0, identity.ResolveInput{Token: result.ResolutionToken, Create: true, AcceptTerms: true})
	if err != nil {
		t.Fatal(err)
	}
	ticket, err = s.Start(ctx, 1, identity.OAuthStart{Provider: "qq"})
	if err != nil {
		t.Fatal(err)
	}
	conflict, err := s.Finish(ctx, 1, identity.OAuthFinish{State: ticket.State, Credential: identity.Credential{Code: "fixture"}})
	var rule mobile.RuleError
	if !errors.As(err, &rule) || rule.Code != "identity_in_use" || conflict.Login != nil || conflict.ResolutionToken != "" {
		t.Fatal("conflict exposed a login or another account", err)
	}
	if err = s.Repo.View(ctx, func(tx mobile.Tx) error {
		link, err := tx.ByKey(mobile.Identities, "qq:fixture-app:fixture-official-subject")
		if err == nil && link.OwnerID != login.User.ID {
			t.Fatal("original identity overwritten")
		}
		return err
	}); err != nil {
		t.Fatal(err)
	}
}
