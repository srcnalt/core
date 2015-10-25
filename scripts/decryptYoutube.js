/**
 * Created by Andrew on 8/18/14.
 */

var DECODE_RULE=[];

function isInteger(n) {
    return typeof n == "number" && isFinite(n) && n % 1 === 0;
}
function isString(s) {
    return (typeof s==='string' || s instanceof String);
}



function findSignatureCode(sourceCode) {
    function findMatch(text, regexp) {
        var matches = text.match(regexp);
        return (matches) ? matches[1] : null;
    }
    var signatureFunctionName =
        findMatch(sourceCode,
            /\.set\s*\("signature"\s*,\s*([a-zA-Z0-9_$][\w$]*)\(/)
        || findMatch(sourceCode,
            /\.sig\s*\|\|\s*([a-zA-Z0-9_$][\w$]*)\(/)
        || findMatch(sourceCode,
            /\.signature\s*=\s*([a-zA-Z_$][\w$]*)\([a-zA-Z_$][\w$]*\)/); //old
    if (signatureFunctionName == null) return 'no sig fuc';
    signatureFunctionName=signatureFunctionName.replace('$','\\$');
    var regCode = new RegExp('function \\s*' + signatureFunctionName +
    '\\s*\\([\\w$]*\\)\\s*{[\\w$]*=[\\w$]*\\.split\\(""\\);(.+);return [\\w$]*\\.join');
    var functionCode = findMatch(sourceCode, regCode);

    if (functionCode == null) return 'no fun code';

    var reverseFunctionName = findMatch(sourceCode,
        /([\w$]*)\s*:\s*function\s*\(\s*[\w$]*\s*\)\s*{\s*(?:return\s*)?[\w$]*\.reverse\s*\(\s*\)\s*}/);

    if (reverseFunctionName) reverseFunctionName = reverseFunctionName.replace('$', '\\$');
    var sliceFunctionName = findMatch(sourceCode,
        /([\w$]*)\s*:\s*function\s*\(\s*[\w$]*\s*,\s*[\w$]*\s*\)\s*{\s*(?:return\s*)?[\w$]*\.(?:slice|splice)\(.+\)\s*}/);
    if (sliceFunctionName) sliceFunctionName = sliceFunctionName.replace('$', '\\$');

    var regSlice = new RegExp('\\.(?:' + 'slice' + (sliceFunctionName ? '|' + sliceFunctionName : '') +
    ')\\s*\\(\\s*(?:[a-zA-Z_$][\\w$]*\\s*,)?\\s*([0-9]+)\\s*\\)'); // .slice(5) sau .Hf(a,5)
    var regReverse = new RegExp('\\.(?:' + 'reverse' + (reverseFunctionName ? '|' + reverseFunctionName : '') +
    ')\\s*\\([^\\)]*\\)'); // .reverse() sau .Gf(a,45)
    var regSwap = new RegExp('[\\w$]+\\s*\\(\\s*[\\w$]+\\s*,\\s*([0-9]+)\\s*\\)');
    var regInline = new RegExp('[\\w$]+\\[0\\]\\s*=\\s*[\\w$]+\\[([0-9]+)\\s*%\\s*[\\w$]+\\.length\\]');
    var functionCodePieces = functionCode.split(';');
    var decodeArray=[];
    for (var i=0; i<functionCodePieces.length; i++) {
        functionCodePieces[i]=functionCodePieces[i].trim();
        var codeLine=functionCodePieces[i];
        if (codeLine.length>0) {
            var arrSlice=codeLine.match(regSlice);
            var arrReverse=codeLine.match(regReverse);
            //debug(i+': '+codeLine+' --'+(arrSlice?' slice length '+arrSlice.length:'') +' '+(arrReverse?'reverse':''));
            if (arrSlice && arrSlice.length >= 2) { // slice
                var slice=parseInt(arrSlice[1], 10);
                if (isInteger(slice)){
                    decodeArray.push(-slice);
                } else break;
            } else if (arrReverse && arrReverse.length >= 1) { // reverse
                decodeArray.push(0);
            } else if (codeLine.indexOf('[0]') >= 0) { // inline swap
                if (i+2<functionCodePieces.length &&
                    functionCodePieces[i+1].indexOf('.length') >= 0 &&
                    functionCodePieces[i+1].indexOf('[0]') >= 0) {
                    var inline=findMatch(functionCodePieces[i+1], regInline);
                    inline=parseInt(inline, 10);
                    decodeArray.push(inline);
                    i+=2;
                } else break;
            } else if (codeLine.indexOf(',') >= 0) { // swap
                var swap=findMatch(codeLine, regSwap);
                swap=parseInt(swap, 10);
                if (isInteger(swap) && swap>0){
                    decodeArray.push(swap);
                } else break;
            } else break;
        }
    }

    if (decodeArray) {

        DECODE_RULE=decodeArray;

    }
}

function decryptSignature(sig) {
    function swap(a,b){var c=a[0];a[0]=a[b%a.length];a[b]=c;return a};
    function decode(sig, arr) { // encoded decryption
        if (!isString(sig)) return null;
        var sigA=sig.split('');
        for (var i=0;i<arr.length;i++) {
            var act=arr[i];
            if (!isInteger(act)) return null;
            sigA=(act>0)?swap(sigA, act):((act==0)?sigA.reverse():sigA.slice(-act));
        }
        var result=sigA.join('');
        return result;
    }

    if (sig==null) return '';
    var arr=DECODE_RULE;
    if (arr) {
        var sig2=decode(sig, arr);
        if (sig2) return sig2;
    } else {
        //setPref(STORAGE_URL, '');
        //setPref(STORAGE_CODE, '');
    }
    return sig;
}





function getWorkingVideo(signature, videoSource) {
    findSignatureCode(videoSource);

     var decrypt = decryptSignature(signature);
    return decrypt;
}
