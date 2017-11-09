var serverStatus = {
    check: function (callback) {
        $.get("/status", callback);
    },
    start: function (selector) {
        setInterval(function () {
            serverStatus.check(function (result) {
                $(selector).html(result);
            })
        }, 1000);

    }
};