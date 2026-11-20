onRecordCreateRequest((e) => {
    if (!e.hasSuperuserAuth()) {
        e.record.getStringSlice("circles").forEach((cid) => {
            const c = $app.findRecordById("circles", cid);
            if (!c.getStringSlice("members").includes(e.auth.id)) {
                throw new BadRequestError("Vous n'êtes pas membre de ce cercle.");
            }
        });
    }
    e.next();
}, "items");

onRecordUpdateRequest((e) => {
    if (!e.hasSuperuserAuth()) {
        e.record.getStringSlice("circles").forEach((cid) => {
            const c = $app.findRecordById("circles", cid);
            if (!c.getStringSlice("members").includes(e.auth.id)) {
                throw new BadRequestError("Vous n'êtes pas membre de ce cercle.");
            }
        });
    }
    e.next();
}, "items");