<%@ page session="false" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" trimDirectiveWhitespaces="true" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>JSP - SpreadSheet</title>
</head>
<body>
<table id="table">
    <tr>
        <th colspan="4">
            <input type="text" disabled title="query" name="query" id ="Query">
        </th>
    </tr>
    <% String columns[]={"A","B","C","D"};%>
    <% for (int i = 1; i<= 4; i++){%>
        <tr>
        <%for (String a : columns) { %>
            <td>
                <input id="<%="C"+a + i%>" title="cell" type="text" name="<%="C" + a + i%>" disabled>
                <div style="display: none;"></div>
            </td>
        <%}%>
        </tr>
    <%}%>
</table>
</body>
<style>
    table, th, td{
        border: 1px solid black;
        padding: 0;
    }
    input {
        outline: none;
        border: none;
        height: 100%;
        width: 100%;
        padding: 0;
        background: #fff;
        color: black;
    }
</style>
<script>
    let current = null;
    let query = document.getElementById('Query');
    function inputListenerFirst() {
        query.value = current.value;
    }
    function inputListenerSecond() {
        current.value = query.value;
    }
    function tableBuild(cells) {
        cells.forEach(c => {
            const e = document.getElementById("C" + c.id);
            e.value = c.value;
            e.nextSibling.value = c.formula;
        });
    }
    window.onload = async (e) => {
        let source = new EventSource('sheet-servlet');
        source.onopen = function(event) {
            console.log("eventsource opened!");
        };
        source.onmessage = function(event) {
            let data = event.data;
            let obj = JSON.parse(data);
            console.log(obj);
            let e = document.getElementById("C" + obj.id);
            e.value = obj.value;
            e.nextSibling.value = obj.formula;
        };
        let res = await fetch('sheet-servlet').then((e) => e.json());
        tableBuild(res);
    };

    document.addEventListener("keypress", async e => {
        if (e.key === "Enter") {
            document.getElementById("table").click();
        }
    })

    document.addEventListener('click', e => {
        if(current !== null && current.nextSibling === e.target) {
            return
        }
        if(current !== null && current !== e.target) {
            current.disabled = true;
            current.parentNode.style = "border: 1px solid black;"
            current.removeEventListener('input', inputListenerFirst);

            fetch('sheet-servlet', {
                method: 'post',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    'id': current.id.substring(1, 3),
                    'value': current.value
                })
            });
        }
        if(current === e.target || String(e.target.id).includes('C')) {
            current = e.target;
            current.disabled = false;
            current.focus();
            query.value = current.nextSibling.value;
            current.value = current.nextSibling.value;

            current.parentNode.style = "border: 2px solid blue;";
            current.addEventListener('input', inputListenerFirst);
            query.addEventListener('input', inputListenerSecond);

        }
        else {
            document.getElementById('Query').value = "";
            let query = document.getElementById('Query');
            query.value = "";
            query.removeEventListener('input', inputListenerSecond);
            current = null;
        }
    })
</script>
</html>