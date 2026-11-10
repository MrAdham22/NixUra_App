/*
 * NixUra email action page (Firebase Hosting, /auth/action).
 *
 * Firebase Authentication emails link here once the console's "Customize action URL" points to
 * https://nixura.web.app/auth/action. Firebase adds ?mode=...&oobCode=...&apiKey=...&lang=... to the link.
 * This page finishes the action with the Identity Toolkit REST API, the same API the app uses
 * (shared/.../data/auth/FirebaseAuthApi.kt):
 *   - verifyEmail, verifyAndChangeEmail, recoverEmail: accounts:update { oobCode }
 *   - resetPassword: accounts:resetPassword { oobCode } to check the link, then { oobCode, newPassword }
 *
 * Nothing is stored: no cookies, no localStorage, no analytics. The one-time code is removed from the
 * address bar as soon as it is read, and only error codes (never the code, key or email) go to the console.
 * continueUrl is ignored on purpose, so the page can never be used to redirect people elsewhere.
 */
(function () {
  "use strict";

  var API = "https://identitytoolkit.googleapis.com/v1/";

  // Mirrors domain/auth/AuthValidation.kt (min 10, max 128, denylist, not the email, at least 4 different characters).
  var PASSWORD_MIN = 10;
  var PASSWORD_MAX = 128;
  var COMMON = [
    "password", "password1", "password12", "password123", "passw0rd", "p@ssw0rd", "qwertyuiop", "qwerty123",
    "qwerty12345", "1234567890", "12345678910", "0123456789", "1q2w3e4r5t", "1qaz2wsx3edc", "iloveyou12",
    "iloveyou123", "letmein123", "welcome123", "welcome1234", "admin12345", "administrator", "abc1234567",
    "abcdefghij", "abcd123456", "monkey1234", "dragon1234", "football123", "baseball123", "superman123",
    "trustno1234", "changeme123", "internet123", "whatever123", "sunshine123", "princess123", "nixura1234",
    "habbit1234", "nixurapassword", "passwordpassword", "1111111111", "0000000000", "aaaaaaaaaa"
  ];

  var TEXT = {
    verifiedTitle: "You're all set!",
    verifiedBody: "Your email is confirmed. Go back to the NixUra app: it moves on by itself, or tap “I've confirmed it”.",
    verifiedNote: "You can close this page.",
    changedEmailTitle: "Email updated",
    changedEmailBody: "Your new email address is confirmed. Use it the next time you sign in to NixUra.",
    recoveredTitle: "Email change undone",
    recoveredBody: "Your account is back on its original email address. If you didn't ask for the change, reset your password in the app to keep your account safe.",
    passwordTitle: "Password changed",
    passwordBody: "Your new password is saved. Go back to the NixUra app and sign in with it.",
    passwordNote: "For your safety, other phones signed in to this account will need to sign in again.",
    resetFor: function (email) { return "For " + email + "."; },
    expiredTitle: "This link has expired",
    expiredBody: "Links in our emails work once and only for a short time. Open NixUra and ask for a new one. If you already confirmed your email, you're all set.",
    badLinkTitle: "This link doesn't work",
    badLinkBody: "It may be incomplete. Open the newest email from NixUra and tap the button there, or ask for a new link in the app.",
    disabledTitle: "This account is turned off",
    disabledBody: "Reply to any NixUra email and we'll help you.",
    notFoundTitle: "We couldn't find this account",
    notFoundBody: "It may have been deleted. You can create a new account in the NixUra app.",
    busyTitle: "Too many tries",
    busyBody: "Please wait a few minutes, then try again.",
    offlineTitle: "No connection",
    offlineBody: "Check your internet connection, then try again.",
    serverTitle: "Something went wrong",
    serverBody: "It's not you, it's us. Please try again in a few minutes.",
    pwEmpty: "Enter a new password.",
    pwShort: "Use at least " + PASSWORD_MIN + " characters.",
    pwLong: "Use at most " + PASSWORD_MAX + " characters.",
    pwCommon: "This password is too common. Try a short sentence instead.",
    pwEmail: "Don't use your email address as your password.",
    pwVariety: "Use a few more different characters.",
    pwMismatch: "The passwords don't match.",
    pwServerWeak: "Choose a stronger password.",
    show: "Show",
    hide: "Hide",
    showLabel: "Show password",
    hideLabel: "Hide password",
    saving: "Saving…",
    save: "Save new password"
  };

  function $(id) { return document.getElementById(id); }

  function show(viewId) {
    ["view-loading", "view-done", "view-reset", "view-error"].forEach(function (id) {
      $(id).hidden = id !== viewId;
    });
  }

  function showDone(title, body, note) {
    $("done-title").textContent = title;
    $("done-body").textContent = body;
    $("done-note").textContent = note || "";
    $("done-note").hidden = !note;
    show("view-done");
  }

  var retryAction = null;

  function showError(kind) {
    var map = {
      expired: [TEXT.expiredTitle, TEXT.expiredBody, false],
      badLink: [TEXT.badLinkTitle, TEXT.badLinkBody, false],
      disabled: [TEXT.disabledTitle, TEXT.disabledBody, false],
      notFound: [TEXT.notFoundTitle, TEXT.notFoundBody, false],
      busy: [TEXT.busyTitle, TEXT.busyBody, true],
      offline: [TEXT.offlineTitle, TEXT.offlineBody, true],
      server: [TEXT.serverTitle, TEXT.serverBody, true]
    };
    var entry = map[kind] || map.server;
    $("error-title").textContent = entry[0];
    $("error-body").textContent = entry[1];
    $("error-retry").hidden = !(entry[2] && retryAction);
    show("view-error");
  }

  /** Firebase error message -> page error kind. WEAK_PASSWORD is handled by the form. */
  function errorKind(code) {
    if (code === "offline") return "offline";
    if (code.indexOf("EXPIRED_OOB_CODE") === 0) return "expired";
    if (code.indexOf("INVALID_OOB_CODE") === 0) return "expired";
    if (code.indexOf("MISSING_OOB_CODE") === 0) return "badLink";
    if (code.indexOf("USER_DISABLED") === 0) return "disabled";
    if (code.indexOf("USER_NOT_FOUND") === 0 || code.indexOf("EMAIL_NOT_FOUND") === 0) return "notFound";
    if (code.indexOf("TOO_MANY_ATTEMPTS") === 0) return "busy";
    return "server";
  }

  /** POSTs to the Identity Toolkit; resolves with the JSON body or rejects with Firebase's error code. */
  function call(apiKey, method, body) {
    return fetch(API + method + "?key=" + encodeURIComponent(apiKey), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      referrerPolicy: "no-referrer",
      credentials: "omit",
      cache: "no-store"
    }).then(function (response) {
      return response.json().catch(function () { return {}; }).then(function (json) {
        if (response.ok) return json;
        var code = (json && json.error && json.error.message) || ("HTTP_" + response.status);
        console.warn("NixUra action failed: " + code);
        throw new Error(code);
      });
    }, function () {
      console.warn("NixUra action failed: offline");
      throw new Error("offline");
    });
  }

  function passwordProblem(password, email) {
    var count = Array.from(password).length;
    if (count === 0) return TEXT.pwEmpty;
    if (count < PASSWORD_MIN) return TEXT.pwShort;
    if (password.length > PASSWORD_MAX) return TEXT.pwLong;
    var lower = password.toLowerCase();
    if (COMMON.indexOf(lower) >= 0) return TEXT.pwCommon;
    var e = (email || "").trim().toLowerCase();
    if (e) {
      var local = e.split("@")[0];
      if (lower === e || (local.length >= 4 && lower === local)) return TEXT.pwEmail;
    }
    if (new Set(password.split("")).size < 4) return TEXT.pwVariety;
    return null;
  }

  function setFieldError(input, errorEl, message) {
    errorEl.textContent = message || "";
    errorEl.hidden = !message;
    input.setAttribute("aria-invalid", message ? "true" : "false");
  }

  function wireToggles() {
    Array.prototype.forEach.call(document.querySelectorAll(".toggle"), function (button) {
      button.addEventListener("click", function () {
        var input = $(button.getAttribute("data-target"));
        var reveal = input.type === "password";
        input.type = reveal ? "text" : "password";
        button.textContent = reveal ? TEXT.hide : TEXT.show;
        button.setAttribute("aria-label", reveal ? TEXT.hideLabel : TEXT.showLabel);
      });
    });
  }

  function applyCode(apiKey, oobCode, mode) {
    retryAction = function () { applyCode(apiKey, oobCode, mode); };
    show("view-loading");
    call(apiKey, "accounts:update", { oobCode: oobCode }).then(function () {
      if (mode === "verifyEmail") showDone(TEXT.verifiedTitle, TEXT.verifiedBody, TEXT.verifiedNote);
      else if (mode === "verifyAndChangeEmail") showDone(TEXT.changedEmailTitle, TEXT.changedEmailBody, TEXT.verifiedNote);
      else showDone(TEXT.recoveredTitle, TEXT.recoveredBody, null);
    }, function (err) { showError(errorKind(err.message)); });
  }

  function startReset(apiKey, oobCode) {
    retryAction = function () { startReset(apiKey, oobCode); };
    show("view-loading");
    call(apiKey, "accounts:resetPassword", { oobCode: oobCode }).then(function (info) {
      var email = (info && info.email) || "";
      $("reset-for").textContent = email ? TEXT.resetFor(email) : "";
      $("reset-for").hidden = !email;
      wireResetForm(apiKey, oobCode, email);
      show("view-reset");
      $("pw").focus();
    }, function (err) { showError(errorKind(err.message)); });
  }

  function wireResetForm(apiKey, oobCode, email) {
    var form = $("reset-form");
    var pw = $("pw");
    var pw2 = $("pw2");
    var submit = $("reset-submit");
    var formError = $("form-error");
    var submitted = false;
    var busy = false;

    function validate() {
      var problem = passwordProblem(pw.value, email);
      var mismatch = !problem && pw2.value !== pw.value ? TEXT.pwMismatch : null;
      if (submitted || pw.value.length >= 4) setFieldError(pw, $("pw-error"), problem);
      if (submitted || pw2.value.length > 0) setFieldError(pw2, $("pw2-error"), mismatch);
      return !problem && !mismatch;
    }

    pw.addEventListener("input", validate);
    pw2.addEventListener("input", validate);

    form.addEventListener("submit", function (event) {
      event.preventDefault();
      if (busy) return;
      submitted = true;
      formError.hidden = true;
      if (!validate()) {
        (pw.getAttribute("aria-invalid") === "true" ? pw : pw2).focus();
        return;
      }
      busy = true;
      submit.disabled = true;
      submit.textContent = TEXT.saving;
      call(apiKey, "accounts:resetPassword", { oobCode: oobCode, newPassword: pw.value }).then(function () {
        pw.value = "";
        pw2.value = "";
        showDone(TEXT.passwordTitle, TEXT.passwordBody, TEXT.passwordNote);
      }, function (err) {
        busy = false;
        submit.disabled = false;
        submit.textContent = TEXT.save;
        var code = err.message;
        if (code.indexOf("WEAK_PASSWORD") === 0) {
          setFieldError(pw, $("pw-error"), TEXT.pwServerWeak);
          pw.focus();
        } else if (code === "offline" || code.indexOf("TOO_MANY_ATTEMPTS") === 0) {
          // Keep the form so nothing typed is lost; say what happened under the button.
          formError.textContent = code === "offline" ? TEXT.offlineBody : TEXT.busyBody;
          formError.hidden = false;
        } else {
          retryAction = null;
          showError(errorKind(code));
        }
      });
    });
  }

  function start() {
    var params = new URLSearchParams(window.location.search);
    var mode = params.get("mode") || "";
    var oobCode = params.get("oobCode") || "";
    var apiKey = params.get("apiKey") || "";

    // The one-time code should not stay in the address bar, history or a screenshot.
    if (window.location.search && window.history && window.history.replaceState) {
      window.history.replaceState(null, "", window.location.pathname);
    }

    wireToggles();
    $("error-retry").addEventListener("click", function () { if (retryAction) retryAction(); });

    var codeOk = /^[A-Za-z0-9_-]{10,1024}$/.test(oobCode);
    var keyOk = /^[A-Za-z0-9_-]{20,100}$/.test(apiKey);
    if (!codeOk || !keyOk) { showError("badLink"); return; }

    if (mode === "verifyEmail" || mode === "verifyAndChangeEmail" || mode === "recoverEmail") {
      applyCode(apiKey, oobCode, mode);
    } else if (mode === "resetPassword") {
      startReset(apiKey, oobCode);
    } else {
      showError("badLink");
    }
  }

  start();
})();
