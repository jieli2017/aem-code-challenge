(function($) {
    "use strict";

    $(document).ready(function() {
        $('.testform form button').on('click', function(e) {
            e.preventDefault();
            $.post('/bin/saveUserDetails', $('test-form form').serialize(), function(data) {
                alert(data);
            });
        });
    });

})(jQuery);