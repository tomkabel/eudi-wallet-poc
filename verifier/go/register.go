package main

import (
	"errors"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/rpregister"
	"log"
	"net/http"
	"reflect"
)

func (p *presenter) authorizeRegister(client string, q oid4vp.DCQL) error {
	if registerAbsent(p.register) {
		return rpregister.ErrNoImplementation
	}
	id := rpregister.RegisterIdentity{UniqueIdentifier: p.clientID, ServiceIdentifier: p.registerService}
	if client != p.clientID {
		return rpregister.ErrUnauthorized
	}
	r, err := p.register.IntendedUse(id)
	if err != nil {
		return err
	}
	if err := r.Validate(id); err != nil {
		return err
	}
	if len(q.Credentials) == 0 {
		return rpregister.ErrUnauthorized
	}
	for _, credential := range q.Credentials {
		if credential.Meta == nil || credential.Meta.DoctypeValue == "" || len(credential.Claims) == 0 || (credential.Format != oid4vp.FormatMsoMdoc && credential.Format != oid4vp.FormatMsoMdocZk) {
			return rpregister.ErrUnauthorized
		}
		for _, claim := range credential.Claims {
			if len(claim.Path) != 2 || claim.Path[0] != credential.Meta.DoctypeValue {
				return rpregister.ErrUnauthorized
			}
			allowed := false
			for _, use := range r.IntendedUses {
				if use.Doctype == credential.Meta.DoctypeValue && use.Predicate == claim.Path[1] {
					allowed = true
					break
				}
			}
			if !allowed {
				return rpregister.ErrUnauthorized
			}
		}
	}
	return nil
}
func (p *presenter) registerGate(w http.ResponseWriter, client string, q oid4vp.DCQL) bool {
	err := p.authorizeRegister(client, q)
	if err == nil {
		return true
	}
	reason := "register-error"
	message := "RP register lookup failed"
	code := http.StatusServiceUnavailable
	switch {
	case errors.Is(err, rpregister.ErrNoImplementation):
		reason = "missing-register-prerequisite"
		message = "RP register implementation is missing"
	case errors.Is(err, rpregister.ErrNotFound):
		reason = "register-not-found"
		message = "RP registration was not found"
		code = http.StatusForbidden
	case errors.Is(err, rpregister.ErrInvalid):
		reason = "register-invalid"
		message = "RP registration is invalid"
		code = http.StatusForbidden
	case errors.Is(err, rpregister.ErrUnauthorized):
		reason = "register-unauthorized"
		message = "RP intended use is unauthorized"
		code = http.StatusForbidden
	}
	log.Printf("rp-register decision=refuse reason=%s", reason)
	writeJSON(w, code, map[string]string{"error": message, "reason": reason})
	return false
}

func registerAbsent(source rpregister.RPRegister) bool {
	if source == nil {
		return true
	}
	v := reflect.ValueOf(source)
	switch v.Kind() {
	case reflect.Chan, reflect.Func, reflect.Interface, reflect.Map, reflect.Pointer, reflect.Slice:
		return v.IsNil()
	}
	return false
}
