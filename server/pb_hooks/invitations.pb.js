routerAdd("POST", "/api/mutualisons/join", (e) => {
    const code = (e.requestInfo().body.code || "").trim().toUpperCase();
    if (!code) throw new BadRequestError("Code manquant.");

    const inv = $app.findFirstRecordByData("invitations", "code", code);
    if (!inv) throw new BadRequestError("Code invalide.");

    if (inv.get("expires_at") && new Date(inv.get("expires_at")) < new Date()) {
        throw new BadRequestError("Code expiré.");
    }
    if (inv.getInt("uses") >= inv.getInt("max_uses")) {
        throw new BadRequestError("Code épuisé.");
    }

    const circle = $app.findRecordById("circles", inv.get("circle"));
    const members = circle.getStringSlice("members");
    if (!members.includes(e.auth.id)) {
        members.push(e.auth.id);
        circle.set("members", members);
        $app.save(circle);
    }

    inv.set("uses", inv.getInt("uses") + 1);
    $app.save(inv);

    return e.json(200, { circle: circle.id, name: circle.get("name") });
}, $apis.requireAuth());