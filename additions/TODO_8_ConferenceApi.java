
        final Queue queue = QueueFactory.getDefaultQueue();
        queue.add(ofy().getTransaction(),
                TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                .param("email", profile.getMainEmail())
                .param("conferenceInfo", conference.toString()));
