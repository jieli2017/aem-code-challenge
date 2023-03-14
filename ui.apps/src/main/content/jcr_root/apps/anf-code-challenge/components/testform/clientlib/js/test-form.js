(function($) {
    "use strict";

    $(document).ready(function() {
        $('.testform form button').on('click', function(e) {
            e.preventDefault();
            var formData = {};
            formData.firstName = $('.testform form input[name="firstName"]').val();
            formData.lastName = $('.testform form input[name="lastName"]').val();
            formData.age = $('.testform form input[name="age"]').val();
            $.ajax({
               type: "POST",
               url: "/bin/saveUserDetails",
               data : JSON.stringify(formData),
               dataType: "json",
               contentType: "application/json",
               success: function (response) {
                  console.log(response);
                  if(response['success']) {
                    $('.testform form input[name="firstName"]').val('')
                    $('.testform form input[name="lastName"]').val('');
                    $('.testform form input[name="age"]').val('');
                    alert('save form successfully!')
                  } else {
                    alert(response['message']);
                  }
               }
            });
        });
    });

})(jQuery);