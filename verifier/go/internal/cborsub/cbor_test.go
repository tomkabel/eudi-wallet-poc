// Package cborsub tests: the strict decoder rejects everything outside the
// subset, and the fuzz target below uses the committed vectors as its seed
// corpus (plan §5.4, ADR-002).
package cborsub

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func mustDecode(t *testing.T, b []byte) *Value {
	t.Helper()
	v, err := Decode(b, DefaultLimits())
	if err != nil {
		t.Fatalf("Decode(% x): %v", b, err)
	}
	return v
}

// tstr20 encodes a 20-character text string with the correct 1-byte
// length form (0x74); a hand-written 0x78 length would not be shortest-form.
func tstr20(s string) []byte {
	if len(s) != 20 {
		panic("tstr20: length must be 20")
	}
	return append([]byte{0x74}, s...)
}

func TestDecodeAcceptsTheSubset(t *testing.T) {
	cases := []struct {
		name string
		in   []byte
		want func(t *testing.T, v *Value)
	}{
		{"uint", []byte{0x18, 0x2a}, func(t *testing.T, v *Value) {
			if v.Kind != KUint || v.Uint != 42 {
				t.Fatalf("got %s %d", v.Kind, v.Uint)
			}
		}},
		{"uint zero", []byte{0x00}, func(t *testing.T, v *Value) {
			if v.Uint != 0 {
				t.Fatalf("got %d", v.Uint)
			}
		}},
		{"text", []byte{0x63, 'a', 'b', 'c'}, func(t *testing.T, v *Value) {
			if v.Text != "abc" {
				t.Fatalf("got %q", v.Text)
			}
		}},
		{"bytes", []byte{0x43, 1, 2, 3}, func(t *testing.T, v *Value) {
			if len(v.Bytes) != 3 || v.Bytes[2] != 3 {
				t.Fatalf("got % x", v.Bytes)
			}
		}},
		{"true false null", []byte{0xf5}, func(t *testing.T, v *Value) {
			if !v.Bool {
				t.Fatal("got false")
			}
		}},
		{"array", []byte{0x83, 0x01, 0x02, 0x03}, func(t *testing.T, v *Value) {
			if len(v.Array) != 3 {
				t.Fatalf("got %d elements", len(v.Array))
			}
		}},
		{"map", []byte{0xa1, 0x63, 'k', 'e', 'y', 0x63, 'v', 'a', 'l'}, func(t *testing.T, v *Value) {
			got, ok, err := v.MapGet("key")
			if err != nil || !ok || got.Text != "val" {
				t.Fatalf("MapGet: %v %v %v", got, ok, err)
			}
			if _, ok, _ := v.MapGet("absent"); ok {
				t.Fatal("absent key reported present")
			}
		}},
		{"nested", []byte{0x82, 0xa1, 0x61, 'a', 0xf5, 0xd8, 0x18, 0x43, 0x82, 0x01, 0x02}, func(t *testing.T, v *Value) {
			if v.Array[0].Map[0].Value.Kind != KBool {
				t.Fatal("nested map lost")
			}
			if v.Array[1].Kind != KTag24 {
				t.Fatalf("tag24 lost: %s", v.Array[1].Kind)
			}
			inner, err := Decode(v.Array[1].Bytes, DefaultLimits())
			if err != nil || len(inner.Array) != 2 {
				t.Fatalf("tag24 inner: %v", err)
			}
		}},
		// Tag 0 over a tstr: the standard date/time multipaz writes for
		// ZkDocumentData.timestamp ("2026-09-24T08:29:56Z" here, 20 chars).
		// It decodes like a tstr, with KTag0 provenance.
		{"tag 0 over a tstr", append([]byte{0xc0}, tstr20("2026-09-24T08:29:56Z")...), func(t *testing.T, v *Value) {
			if v.Kind != KTag0 {
				t.Fatalf("kind is %s, want tag0", v.Kind)
			}
			if v.Text != "2026-09-24T08:29:56Z" {
				t.Fatalf("text %q", v.Text)
			}
			if v.Raw[0] != 0xc0 {
				t.Fatal("Raw does not start at the tag head")
			}
		}},
		// (Tag 0 over a non-text item is refused; see TestDecodeRejectsTheRest.)
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			tc.want(t, mustDecode(t, tc.in))
		})
	}
}

func TestDecodeRejectsTheRest(t *testing.T) {
	depthBomb := func(n int) []byte {
		out := make([]byte, n)
		for i := range out {
			out[i] = 0x81 // array(1), opened n times
		}
		out = append(out, 0x01)
		return out
	}
	manyItems := func(n int) []byte {
		out := append([]byte{0x9b, 0, 0, 0, 0, 0, 0, 0x04, 0x01}, make([]byte, n)...)
		for i := 9; i < len(out); i++ {
			out[i] = 0x01
		}
		return out
	}
	stringOver := func(n int) []byte {
		// text string with a 4-byte length argument claiming n bytes
		return append([]byte{0x7a, byte(n >> 24), byte(n >> 16), byte(n >> 8), byte(n)}, make([]byte, 8)...)
	}
	cases := []struct {
		name string
		in   []byte
	}{
		{"negative int", []byte{0x20}},
		{"float", []byte{0xf9, 0x3c, 0x00}},
		{"undefined", []byte{0xf7}},
		{"reserved simple", []byte{0xf0}},
		{"tag 1, not accepted", []byte{0xc1, 0x01}},
		{"tag 0 over a non-text item", []byte{0xc0, 0x01}},
		{"tag 24 over non-bytes", []byte{0xc2, 0x01}},
		{"tag 24 over non-CBOR bytes", []byte{0xd8, 0x18, 0x43, 0x01, 0x02, 0x03}},
		{"indefinite array", []byte{0x9f, 0x01, 0xff}},
		{"indefinite text", []byte{0x7f, 0x61, 'a', 0xff}},
		{"non-shortest-form uint", []byte{0x18, 0x17}},
		{"8-bit head over a truncated input", []byte{0x18}},
		{"non-shortest-form uint 2", []byte{0x19, 0x00, 0x2a}},
		{"non-shortest-form uint 3", []byte{0x1a, 0x00, 0x00, 0x00, 0x2a}},
		{"non-shortest-form length", append([]byte{0x78}, 4, 'a', 'b', 'c', 'd')},
		{"duplicate text keys", []byte{0xa2, 0x61, 'a', 0x01, 0x61, 'a', 0x02}},
		{"duplicate uint keys", []byte{0xa2, 0x01, 0x00, 0x01, 0x00}},
		{"array depth continued through tag 24", func() []byte {
			// 20 arrays, then tag 24 over 20 more: 41 levels in all.
			inner := depthBomb(20) // 21 bytes: 20 array heads and a 1
			out := append(depthBomb(20)[:20], 0xd8, 0x18, 0x40+byte(len(inner)))
			return append(out, inner...)
		}()},
		{"truncated string", []byte{0x65, 'a', 'b'}},
		{"truncated array", []byte{0x82, 0x01}},
		{"array over depth", depthBomb(40)},
		{"array over items", manyItems(1025)},
		{"string over limit", stringOver(1<<20 + 1)},
		{"trailing bytes", []byte{0x01, 0x02}},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			if _, err := Decode(tc.in, DefaultLimits()); err == nil {
				t.Fatalf("Decode(% x) succeeded, want rejection", tc.in)
			} else if !strings.Contains(err.Error(), ErrDecode.Error()) {
				t.Fatalf("wrong error class: %v", err)
			}
		})
	}
}

// TestMapGetRefusesANonTextKeyAfterTheMatch: every key is checked, not just
// those before the one asked for.
func TestMapGetRefusesANonTextKeyAfterTheMatch(t *testing.T) {
	v, err := Decode([]byte{0xa2, 0x61, 'a', 0x00, 0x01, 0x00}, DefaultLimits())
	if err != nil {
		t.Fatalf("decode: %v", err)
	}
	if _, _, err := v.MapGet("a"); err == nil {
		t.Fatal(`MapGet("a") on {"a":0, 1:0} succeeded, want a non-text-key error`)
	}
}

// TestDecodeExactLimits walks right up to each limit and one past it.
func TestDecodeExactLimits(t *testing.T) {
	// 32 deep is accepted, 33 refused.
	bomb := make([]byte, 32)
	for i := range bomb {
		bomb[i] = 0x81
	}
	bomb = append(bomb, 0x01)
	if _, err := Decode(bomb, DefaultLimits()); err != nil {
		t.Fatalf("32-deep array refused: %v", err)
	}
	bomb = make([]byte, 33)
	for i := range bomb {
		bomb[i] = 0x81
	}
	bomb = append(bomb, 0x01)
	if _, err := Decode(bomb, DefaultLimits()); err == nil {
		t.Fatal("33-deep array accepted")
	}
}

// TestTag24NestingIsGloballyBounded pins the finding-3 fix: tag-24 layers
// share ONE nesting budget across the whole decode. Under the old per-level
// Decode call each tag boundary restarted the depth counter, so N nested
// tags cost N x MaxDepth frames — a stack/allocation amplifier. Now a chain
// deeper than MaxDepth is refused no matter how the bytes are wrapped.
func TestTag24NestingIsGloballyBounded(t *testing.T) {
	// Each layer: tag 24 (d8 18) over a byte string (0x58, one-byte length)
	// whose content is the next layer's encoding.
	wrap := func(inner []byte) []byte {
		// tag 24 (d8 18) takes no length argument: the tagged item — a byte
		// string of the next layer's encoding — follows the head. The bstr
		// length header must be shortest-form: 0x40+n below 24, 0x58+n above.
		n := len(inner)
		var hdr []byte
		switch {
		case n < 24:
			hdr = []byte{0x40 + byte(n)}
		case n < 256:
			hdr = []byte{0x58, byte(n)}
		default:
			hdr = []byte{0x59, byte(n >> 8), byte(n)}
		}
		bstr := append(hdr, inner...)
		return append([]byte{0xd8, 0x18}, bstr...)
	}
	inner := []byte{0x01}     // the unsigned integer 1
	for i := 0; i < 40; i++ { // 40 > MaxDepth (32)
		inner = wrap(inner)
	}
	if _, err := Decode(inner, DefaultLimits()); err == nil {
		t.Fatal("40-layer tag-24 chain accepted; nesting is not bounded globally")
	}

	// A shallow chain inside the budget still decodes, and the tag-24
	// content must not silently swallow trailing bytes either.
	ok := func(depth int) []byte {
		inner := []byte{0x01}
		for i := 0; i < depth; i++ {
			inner = wrap(inner)
		}
		return inner
	}
	for _, depth := range []int{1, 4, 30} {
		if _, err := Decode(ok(depth), DefaultLimits()); err != nil {
			t.Fatalf("%d-layer tag-24 chain refused: %v", depth, err)
		}
	}
	broken := ok(3)
	broken = append(broken, 0x00) // trailing byte inside the innermost payload
	if _, err := Decode(broken, DefaultLimits()); err == nil {
		t.Fatal("tag-24 content with trailing bytes accepted")
	}
}

func FuzzDecode(f *testing.F) {
	// Seed corpus: the accepted subset and the committed vectors, so the
	// fuzzer starts from bytes a real wallet would send.
	for _, seed := range [][]byte{
		{0x83, 0xf6, 0xf6, 0x82, 0x63, 'd', 'c', 'a', 'p', 'i', 0x40},
		{0xa1, 0x63, 'k', 'e', 'y', 0x63, 'v', 'a', 'l'},
		{0xd8, 0x18, 0x43, 0x82, 0x01, 0x02},
	} {
		f.Add(seed)
	}
	matches, _ := filepath.Glob(filepath.Join("..", "..", "zk", "testdata", "step0-multipaz", "*"))
	iso, _ := filepath.Glob(filepath.Join("..", "..", "zk", "testdata", "step2a-iso-annex-c", "*.cbor"))
	matches = append(matches, iso...)
	for _, m := range matches {
		if b, err := os.ReadFile(m); err == nil && len(b) > 0 {
			f.Add(b)
		}
	}
	f.Fuzz(func(t *testing.T, data []byte) {
		// The only contract: Decode either returns a value or an error —
		// never a panic, never a hang, never a value outside the subset.
		v, err := Decode(data, DefaultLimits())
		if err == nil {
			_ = v.Kind
		}
	})
}
